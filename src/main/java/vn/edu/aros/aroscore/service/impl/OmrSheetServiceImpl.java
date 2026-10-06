package vn.edu.aros.aroscore.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.client.OmrEngineClient;
import vn.edu.aros.aroscore.config.OmrProperties;
import vn.edu.aros.aroscore.dto.matrix.QuestionMatrix;
import vn.edu.aros.aroscore.dto.omr.OmrScanResponse;
import vn.edu.aros.aroscore.dto.request.OmrReviewRequest;
import vn.edu.aros.aroscore.dto.response.OmrAnswerItemResponse;
import vn.edu.aros.aroscore.dto.response.OmrBubbleResponse;
import vn.edu.aros.aroscore.dto.response.OmrSheetResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamSessionStatus;
import vn.edu.aros.aroscore.entity.enums.OmrSheetStatus;
import vn.edu.aros.aroscore.exception.OmrEngineException;
import vn.edu.aros.aroscore.repository.*;
import vn.edu.aros.aroscore.service.OmrSheetService;
import vn.edu.aros.aroscore.service.omr.OmrGradingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OmrSheetServiceImpl implements OmrSheetService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png");

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final OMRFileRepository omrFileRepository;
    private final ExamVersionRepository examVersionRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final OmrEngineClient omrEngineClient;
    private final OmrGradingService omrGradingService;
    private final OmrProperties omrProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Load exam + questions, rồi fetch options ở query riêng (tránh MultipleBagFetchException). */
    private Exam loadExamWithQuestionsAndOptions(Long examId) {
        Exam exam = examRepository.findByIdWithQuestions(examId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi!"));
        List<Long> questionIds = exam.getExamQuestions() == null
                ? List.of()
                : exam.getExamQuestions().stream()
                .map(eq -> eq.getQuestion().getId())
                .distinct()
                .toList();
        if (!questionIds.isEmpty()) {
            questionRepository.findByIdInWithOptions(questionIds);
        }
        // Touch classrooms trong transaction để match MSSV theo lớp phiên / đề
        if (exam.getClassrooms() != null) {
            exam.getClassrooms().size();
        }
        return exam;
    }

    /**
     * Match MSSV trong lớp của phiên chấm (fallback: lớp được giao đề).
     */
    private User resolveStudentForSheet(OMRFile sheet, String studentCode) {
        Long classroomId = null;
        if (sheet.getExamSession() != null && sheet.getExamSession().getClassroom() != null) {
            classroomId = sheet.getExamSession().getClassroom().getId();
        } else if (sheet.getExam() != null && sheet.getExam().getClassrooms() != null) {
            classroomId = sheet.getExam().getClassrooms().stream()
                    .map(Classroom::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (classroomId == null) {
            return null;
        }
        List<User> matches = userRepository.findByStudentCodeInClassrooms(List.of(classroomId), studentCode);
        if (matches.size() == 1) {
            return matches.getFirst();
        }
        return null;
    }

    @Override
    @Transactional
    public OmrSheetResponse uploadAndScan(Long sessionId, MultipartFile file) {
        ExamSession session = examSessionRepository.findByIdAndTeacherEmail(sessionId, currentEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chấm hoặc bạn không có quyền!"));

        if (session.getStatus() != ExamSessionStatus.OPEN) {
            throw new RuntimeException("Phiên chấm đã đóng, không thể upload thêm phiếu!");
        }

        validateFile(file);
        Long teacherId = session.getExam().getTeacher().getId();
        Exam exam = loadExamWithQuestionsAndOptions(session.getExam().getId());
        session.setExam(exam);

        final byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Không đọc được file ảnh!");
        }
        String contentHash = sha256Hex(fileBytes);
        if (omrFileRepository.existsDuplicateInSession(
                sessionId,
                contentHash,
                List.of(OmrSheetStatus.GRADED, OmrSheetStatus.NEEDS_REVIEW, OmrSheetStatus.PROCESSING))) {
            throw new IllegalArgumentException(
                    "Ảnh này đã được quét trong phiên chấm. Không tạo bài nộp mới.");
        }

        OMRFile sheet = OMRFile.builder()
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "sheet.jpg")
                .filePath("pending")
                .contentHash(contentHash)
                .exam(exam)
                .examSession(session)
                .status(OmrSheetStatus.PROCESSING)
                .maxScore(exam.getMaxScore())
                .build();
        sheet = omrFileRepository.save(sheet);

        String teacherIdStr = String.valueOf(teacherId);
        String sessionIdStr = String.valueOf(sessionId);
        String submissionIdStr = String.valueOf(sheet.getId());

        Path savedPath = storeFile(file, sheet.getId());
        sheet.setFilePath(savedPath.toString().replace("\\", "/"));
        omrFileRepository.save(sheet);

        try {
            OmrEngineClient.OmrScanResult scanResult = omrEngineClient.scan(
                    savedPath, teacherIdStr, sessionIdStr, submissionIdStr);
            if (!scanResult.success() || scanResult.response() == null) {
                sheet.setStatus(OmrSheetStatus.FAILED);
                omrFileRepository.save(sheet);
                throw new RuntimeException("OMR Engine lỗi: " + scanResult.errorMessage());
            }
            return applyScanResult(sheet, scanResult.response());
        } catch (OmrEngineException e) {
            boolean retake = e.getHttpStatus() == 422
                    || (e.getErrorCode() != null && e.getErrorCode().startsWith("OMR_RET_"));
            sheet.setStatus(retake ? OmrSheetStatus.RETAKE_REQUIRED : OmrSheetStatus.FAILED);
            try {
                sheet.setOmrRaw(objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "error_code", e.getErrorCode(),
                        "message", e.getMessage() != null ? e.getMessage() : "",
                        "hint", e.getHint() != null ? e.getHint() : "",
                        "details", e.getDetails() != null ? e.getDetails() : Map.of()
                )));
            } catch (JsonProcessingException ignored) {
                sheet.setOmrRaw(e.getMessage());
            }
            omrFileRepository.save(sheet);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OmrSheetResponse getSheet(Long sheetId) {
        OMRFile sheet = omrFileRepository.findByIdWithDetails(sheetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu OMR!"));
        assertTeacherAccess(sheet);
        return toResponse(sheet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OmrSheetResponse> listBySession(Long sessionId) {
        examSessionRepository.findByIdAndTeacherEmail(sessionId, currentEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chấm hoặc bạn không có quyền!"));
        return omrFileRepository.findByExamSessionIdOrderByUploadTimeDesc(sessionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OmrSheetResponse reviewAndRegrade(Long sheetId, OmrReviewRequest request) {
        OMRFile sheet = omrFileRepository.findByIdWithDetails(sheetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu OMR!"));
        assertTeacherAccess(sheet);

        if (sheet.getDetectedExamCode() == null || sheet.getDetectedExamCode().isBlank()) {
            throw new RuntimeException("Phiếu chưa có mã đề, không thể chấm lại!");
        }

        Exam exam = loadExamWithQuestionsAndOptions(sheet.getExam().getId());
        sheet.setExam(exam);

        // Giữ bubble overlay + đáp án cũ; chỉ ghi đè câu GV sửa
        Map<Integer, String> mergedAnswers = new LinkedHashMap<>();
        Map<Integer, String> preservedBubbles = new HashMap<>();
        Map<Integer, String> preservedOverlays = new HashMap<>();
        if (sheet.getAnswerDetails() != null) {
            for (OmrAnswerDetail detail : sheet.getAnswerDetails()) {
                if (detail.getChosen() != null) {
                    mergedAnswers.put(detail.getQuestionNumber(), detail.getChosen());
                }
                if (detail.getBubbleJson() != null && !detail.getBubbleJson().isBlank()) {
                    preservedBubbles.put(detail.getQuestionNumber(), detail.getBubbleJson());
                }
                if (detail.getMarkedBubblesJson() != null && !detail.getMarkedBubblesJson().isBlank()) {
                    preservedOverlays.put(detail.getQuestionNumber(), detail.getMarkedBubblesJson());
                }
            }
        }
        if (request.getAnswers() != null) {
            mergedAnswers.putAll(request.getAnswers());
        }

        OmrGradingService.GradingOutcome outcome = omrGradingService.regradeManual(
                exam, sheet.getDetectedExamCode(), mergedAnswers, preservedBubbles, preservedOverlays);

        applyGradingOutcome(sheet, outcome, false);
        syncSubmission(sheet, outcome);
        return toResponse(sheet);
    }

    private OmrSheetResponse applyScanResult(OMRFile sheet, OmrScanResponse scan) {
        try {
            return applyScanResultInternal(sheet, scan);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý kết quả OMR!");
        }
    }

    private OmrSheetResponse applyScanResultInternal(OMRFile sheet, OmrScanResponse scan)
            throws JsonProcessingException {
        sheet.setOmrRaw(objectMapper.writeValueAsString(scan));

        String warpedUrl = scan.getImages() != null ? scan.getImages().getWarpedUrl() : null;
        sheet.setWarpedUrl(warpedUrl);
        if (scan.getImages() != null && scan.getImages().getCloudFolder() != null) {
            sheet.setCloudFolder(scan.getImages().getCloudFolder());
        }

        String detectedStudentId = scan.getSummary() != null && scan.getSummary().getStudentId() != null
                ? scan.getSummary().getStudentId()
                : (scan.getStudentId() != null ? scan.getStudentId().getValue() : null);
        String detectedExamCode = scan.getSummary() != null && scan.getSummary().getExamCode() != null
                ? scan.getSummary().getExamCode()
                : (scan.getExamCode() != null ? scan.getExamCode().getValue() : null);

        sheet.setDetectedStudentId(detectedStudentId);
        sheet.setDetectedExamCode(detectedExamCode);

        if (detectedStudentId == null || detectedStudentId.isBlank()) {
            failSheet(sheet, "Không đọc được mã sinh viên trên phiếu!");
        }
        if (scan.getSummary() != null && Boolean.FALSE.equals(scan.getSummary().getStudentIdValid())) {
            failSheet(sheet, "Mã sinh viên trên phiếu không hợp lệ: " + detectedStudentId);
        }
        if (scan.getStudentId() != null && Boolean.FALSE.equals(scan.getStudentId().getValid())) {
            failSheet(sheet, "Mã sinh viên trên phiếu không hợp lệ: " + detectedStudentId);
        }

        User student = resolveStudentForSheet(sheet, detectedStudentId);
        if (student == null) {
            failSheet(sheet,
                    "Không tìm thấy sinh viên mã \"" + detectedStudentId
                            + "\" trong lớp của phiên chấm này!");
        }
        sheet.setStudent(student);

        // Mỗi SV chỉ quét 1 lần / phiên (FAILED / RETAKE_REQUIRED được thử lại)
        if (sheet.getExamSession() != null
                && omrFileRepository.existsStudentSheetInSession(
                sheet.getExamSession().getId(),
                student.getId(),
                sheet.getId(),
                List.of(OmrSheetStatus.GRADED, OmrSheetStatus.NEEDS_REVIEW, OmrSheetStatus.PROCESSING))) {
            failSheet(sheet,
                    "Sinh viên \"" + detectedStudentId
                            + "\" đã có phiếu trong phiên chấm này. Không quét lại.");
        }

        if (detectedExamCode == null || detectedExamCode.isBlank()) {
            failSheet(sheet, "Không đọc được mã đề trên phiếu!");
        }
        if (scan.getSummary() != null && Boolean.FALSE.equals(scan.getSummary().getExamCodeValid())) {
            failSheet(sheet, "Mã đề trên phiếu không hợp lệ: " + detectedExamCode);
        }
        if (scan.getExamCode() != null && Boolean.FALSE.equals(scan.getExamCode().getValid())) {
            failSheet(sheet, "Mã đề trên phiếu không hợp lệ: " + detectedExamCode);
        }
        if (!examVersionRepository.existsByExamIdAndVersionCode(sheet.getExam().getId(), detectedExamCode)) {
            failSheet(sheet,
                    "Mã đề \"" + detectedExamCode + "\" không thuộc đề thi này!");
        }

        List<Integer> engineNeedReview = scan.getAnswers() != null ? scan.getAnswers().getNeedReview() : null;
        OmrGradingService.GradingOutcome outcome = omrGradingService.grade(
                sheet.getExam(), detectedExamCode, scan, engineNeedReview);

        // Quét xong luôn cần GV xem lại; điểm chính thức ghi khi bấm Lưu kết quả
        applyGradingOutcome(sheet, outcome, true);
        return toResponse(sheet);
    }

    /** Đánh dấu FAILED rồi ném 400 cho Frontend. */
    private void failSheet(OMRFile sheet, String message) {
        sheet.setStatus(OmrSheetStatus.FAILED);
        try {
            omrFileRepository.save(sheet);
        } catch (Exception ignored) {
            // vẫn báo lỗi cho FE
        }
        throw new IllegalArgumentException(message);
    }

    private void applyGradingOutcome(OMRFile sheet, OmrGradingService.GradingOutcome outcome, boolean needsReview) {
        try {
            applyGradingOutcomeInternal(sheet, outcome, needsReview);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi lưu kết quả chấm!");
        }
    }

    private void applyGradingOutcomeInternal(OMRFile sheet, OmrGradingService.GradingOutcome outcome, boolean needsReview)
            throws JsonProcessingException {
        sheet.getAnswerDetails().clear();
        for (OmrGradingService.GradedAnswer ga : outcome.answers()) {
            sheet.addAnswerDetail(OmrAnswerDetail.builder()
                    .questionNumber(ga.questionNumber())
                    .chosen(ga.chosen())
                    .correctAnswer(ga.correctAnswer())
                    .isCorrect(ga.isCorrect())
                    .omrStatus(ga.omrStatus())
                    .omrLevel(ga.level())
                    .omrColor(ga.color())
                    .bubbleJson(ga.bubbleJson())
                    .markedBubblesJson(ga.markedBubblesJson())
                    .build());
        }

        sheet.setScore(outcome.score());
        sheet.setMaxScore(outcome.maxScore());
        sheet.setNeedReviewJson(objectMapper.writeValueAsString(outcome.needReview()));
        sheet.setGradedAt(LocalDateTime.now());
        sheet.setStatus(needsReview ? OmrSheetStatus.NEEDS_REVIEW : OmrSheetStatus.GRADED);
        omrFileRepository.save(sheet);
    }

    private void syncSubmission(OMRFile sheet, OmrGradingService.GradingOutcome outcome) {
        if (sheet.getStudent() == null || sheet.getDetectedExamCode() == null) {
            return;
        }

        Exam exam = sheet.getExam();
        User student = sheet.getStudent();
        Classroom classroom = sheet.getExamSession() != null ? sheet.getExamSession().getClassroom() : null;
        if (classroom == null) {
            throw new IllegalArgumentException(
                    "Phiên chấm chưa gắn lớp — không thể ghi điểm theo lớp. Tạo lại phiên với classroomId.");
        }

        // Cập nhật submission theo đề + SV + lớp; quét lại cùng lớp chỉ cập nhật, không nhân bản
        Submission submission = submissionRepository
                .findAllByStudentIdAndExamIdAndClassroomId(student.getId(), exam.getId(), classroom.getId())
                .stream()
                .max(Comparator.comparing(
                        s -> s.getSubmitTime() != null
                                ? s.getSubmitTime()
                                : (s.getStartTime() != null ? s.getStartTime() : LocalDateTime.MIN)))
                .orElse(null);

        if (submission == null) {
            submission = Submission.builder()
                    .exam(exam)
                    .student(student)
                    .classroom(classroom)
                    .attemptNo(1)
                    .versionCode(sheet.getDetectedExamCode())
                    .startTime(sheet.getUploadTime() != null ? sheet.getUploadTime() : LocalDateTime.now())
                    .build();
        }

        submission.setClassroom(classroom);
        submission.setVersionCode(sheet.getDetectedExamCode());
        submission.setScore(outcome.score());
        submission.setSubmitTime(LocalDateTime.now());
        if (submission.getStartTime() == null) {
            submission.setStartTime(sheet.getUploadTime() != null ? sheet.getUploadTime() : LocalDateTime.now());
        }

        Map<Integer, Long> orderToQuestionId = loadOrderToQuestionId(exam.getId(), sheet.getDetectedExamCode());
        submission.getDetails().clear();
        for (OmrGradingService.GradedAnswer ga : outcome.answers()) {
            Long questionId = orderToQuestionId.get(ga.questionNumber());
            if (questionId == null) {
                continue;
            }
            Question question = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(questionId))
                    .map(ExamQuestion::getQuestion)
                    .findFirst()
                    .orElse(null);
            if (question == null) {
                continue;
            }
            submission.addDetail(SubmissionDetail.builder()
                    .question(question)
                    .selectedAnswer(ga.chosen())
                    .isCorrect(ga.isCorrect())
                    .build());
        }

        submission = submissionRepository.save(submission);
        sheet.setSubmission(submission);
        omrFileRepository.save(sheet);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    private Map<Integer, Long> loadOrderToQuestionId(Long examId, String versionCode) {
        ExamVersion version = examVersionRepository.findByExamIdAndVersionCode(examId, versionCode).orElseThrow();
        try {
            List<QuestionMatrix> matrix = objectMapper.readValue(version.getShuffleMatrix(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionMatrix.class));
            return matrix.stream()
                    .collect(Collectors.toMap(QuestionMatrix::getNewOrder, QuestionMatrix::getOriginalQuestionId));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi đọc ma trận mã đề!");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh không được để trống!");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Chỉ hỗ trợ ảnh JPG/PNG!");
        }
        long maxBytes = omrProperties.getUpload().getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new RuntimeException("Ảnh vượt quá " + omrProperties.getUpload().getMaxSizeMb() + "MB!");
        }
    }

    private Path storeFile(MultipartFile file, Long sheetId) {
        try {
            Path dir = Paths.get(omrProperties.getUpload().getDir());
            Files.createDirectories(dir);
            String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                    : ".jpg";
            Path target = dir.resolve("sheet_" + sheetId + ext);
            file.transferTo(target);
            return target.toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Không lưu được ảnh phiếu: " + e.getMessage());
        }
    }

    private void assertTeacherAccess(OMRFile sheet) {
        if (!sheet.getExam().getTeacher().getEmail().equals(currentEmail())) {
            throw new RuntimeException("Bạn không có quyền xem phiếu này!");
        }
    }

    private OmrSheetResponse toResponse(OMRFile sheet) {
        List<Integer> needReview = parseNeedReview(sheet.getNeedReviewJson());
        List<OmrAnswerItemResponse> answers = sheet.getAnswerDetails() == null
                ? List.of()
                : sheet.getAnswerDetails().stream()
                .sorted(Comparator.comparing(OmrAnswerDetail::getQuestionNumber))
                .map(d -> {
                    OmrBubbleResponse bubble = parseBubble(d.getBubbleJson());
                    List<OmrBubbleResponse> overlayBubbles = parseOverlayList(d.getMarkedBubblesJson());
                    return OmrAnswerItemResponse.builder()
                            .question(d.getQuestionNumber())
                            .chosen(d.getChosen())
                            .correctAnswer(d.getCorrectAnswer())
                            .isCorrect(d.getIsCorrect())
                            .status(d.getOmrStatus())
                            .level(d.getOmrLevel())
                            .color(d.getOmrColor())
                            .bubble(bubble)
                            .overlayBubbles(overlayBubbles)
                            .build();
                })
                .toList();

        return OmrSheetResponse.builder()
                .submissionId(sheet.getId())
                .examSessionId(sheet.getExamSession() != null ? sheet.getExamSession().getId() : null)
                .examId(sheet.getExam().getId())
                .status(sheet.getStatus())
                .studentId(sheet.getDetectedStudentId())
                .matchedStudentId(sheet.getStudent() != null ? sheet.getStudent().getId() : null)
                .studentName(sheet.getStudent() != null ? sheet.getStudent().getFullName() : null)
                .examCode(sheet.getDetectedExamCode())
                .score(sheet.getScore())
                .maxScore(sheet.getMaxScore())
                .warpedUrl(sheet.getWarpedUrl())
                .cloudFolder(sheet.getCloudFolder())
                .originalImageUrl(sheet.getFilePath())
                .needReview(needReview)
                .answers(answers)
                .gradedAt(sheet.getGradedAt())
                .build();
    }

    private OmrBubbleResponse parseBubble(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            var b = objectMapper.readValue(json, OmrScanResponse.OmrBubble.class);
            return toBubbleResponse(b);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private List<OmrBubbleResponse> parseOverlayList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<OmrBubbleResponse> list = objectMapper.readValue(json,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, OmrBubbleResponse.class));
            return list != null ? list : List.of();
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private OmrBubbleResponse toBubbleResponse(OmrScanResponse.OmrBubble b) {
        if (b == null) {
            return null;
        }
        return OmrBubbleResponse.builder()
                .choice(b.getChoice())
                .x(b.getX())
                .y(b.getY())
                .w(b.getW())
                .h(b.getH())
                .build();
    }

    private List<Integer> parseNeedReview(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
