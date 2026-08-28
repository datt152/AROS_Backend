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
import vn.edu.aros.aroscore.repository.*;
import vn.edu.aros.aroscore.service.OmrSheetService;
import vn.edu.aros.aroscore.service.omr.OmrGradingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Override
    @Transactional
    public OmrSheetResponse uploadAndScan(Long sessionId, MultipartFile file) {
        ExamSession session = examSessionRepository.findByIdAndTeacherEmail(sessionId, currentEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chấm hoặc bạn không có quyền!"));

        if (session.getStatus() != ExamSessionStatus.OPEN) {
            throw new RuntimeException("Phiên chấm đã đóng, không thể upload thêm phiếu!");
        }

        validateFile(file);
        Exam exam = examRepository.findByIdWithQuestions(session.getExam().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi!"));
        session.setExam(exam);

        OMRFile sheet = OMRFile.builder()
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "sheet.jpg")
                .filePath("pending")
                .exam(exam)
                .examSession(session)
                .status(OmrSheetStatus.PROCESSING)
                .maxScore(exam.getMaxScore())
                .build();
        sheet = omrFileRepository.save(sheet);

        Path savedPath = storeFile(file, sheet.getId());
        sheet.setFilePath(savedPath.toString().replace("\\", "/"));
        omrFileRepository.save(sheet);

        try {
            OmrEngineClient.OmrScanResult scanResult = omrEngineClient.scan(savedPath, sheet.getId());
            if (!scanResult.success() || scanResult.response() == null) {
                sheet.setStatus(OmrSheetStatus.FAILED);
                omrFileRepository.save(sheet);
                throw new RuntimeException("OMR Engine lỗi: " + scanResult.errorMessage());
            }
            return applyScanResult(sheet, scanResult.response());
        } catch (OmrEngineClient.OmrRetakeRequiredException e) {
            sheet.setStatus(OmrSheetStatus.RETAKE_REQUIRED);
            sheet.setOmrRaw(e.getMessage());
            omrFileRepository.save(sheet);
            throw new RuntimeException("Cần chụp lại ảnh phiếu: " + e.getMessage());
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

        Exam exam = examRepository.findByIdWithQuestions(sheet.getExam().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi!"));
        sheet.setExam(exam);

        OmrGradingService.GradingOutcome outcome = omrGradingService.regradeManual(
                exam, sheet.getDetectedExamCode(), request.getAnswers());

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

        String detectedStudentId = scan.getSummary() != null && scan.getSummary().getStudentId() != null
                ? scan.getSummary().getStudentId()
                : (scan.getStudentId() != null ? scan.getStudentId().getValue() : null);
        String detectedExamCode = scan.getSummary() != null && scan.getSummary().getExamCode() != null
                ? scan.getSummary().getExamCode()
                : (scan.getExamCode() != null ? scan.getExamCode().getValue() : null);

        sheet.setDetectedStudentId(detectedStudentId);
        sheet.setDetectedExamCode(detectedExamCode);

        boolean needsReview = false;
        if (detectedStudentId == null || detectedStudentId.isBlank()
                || (scan.getSummary() != null && Boolean.FALSE.equals(scan.getSummary().getStudentIdValid()))) {
            needsReview = true;
        }
        if (detectedExamCode == null || detectedExamCode.isBlank()
                || !examVersionRepository.existsByExamIdAndVersionCode(sheet.getExam().getId(), detectedExamCode)) {
            needsReview = true;
        }

        User student = null;
        if (detectedStudentId != null && !detectedStudentId.isBlank()) {
            student = userRepository.findByStudentCode(detectedStudentId).orElse(null);
            if (student == null) {
                needsReview = true;
            } else {
                sheet.setStudent(student);
            }
        }

        if (detectedExamCode == null || !examVersionRepository.existsByExamIdAndVersionCode(
                sheet.getExam().getId(), detectedExamCode)) {
            sheet.setStatus(OmrSheetStatus.NEEDS_REVIEW);
            sheet.setNeedReviewJson(objectMapper.writeValueAsString(
                    scan.getAnswers() != null ? scan.getAnswers().getNeedReview() : List.of()));
            omrFileRepository.save(sheet);
            return toResponse(sheet);
        }

        List<Integer> engineNeedReview = scan.getAnswers() != null ? scan.getAnswers().getNeedReview() : null;
        OmrGradingService.GradingOutcome outcome = omrGradingService.grade(
                sheet.getExam(), detectedExamCode, scan, engineNeedReview);

        if (!outcome.needReview().isEmpty()) {
            needsReview = true;
        }
        for (OmrGradingService.GradedAnswer ga : outcome.answers()) {
            if (ga.needsReview()) {
                needsReview = true;
                break;
            }
        }

        applyGradingOutcome(sheet, outcome, needsReview);
        syncSubmission(sheet, outcome);
        return toResponse(sheet);
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
                    .bubbleJson(ga.bubbleJson())
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

        Submission submission = submissionRepository
                .findByExamAndStudentAndSubmitTimeIsNull(exam, student)
                .orElseGet(() -> submissionRepository.findAllByStudentIdAndExamIdIn(
                                student.getId(), List.of(exam.getId())).stream()
                        .filter(s -> s.getSubmitTime() != null)
                        .findFirst()
                        .orElse(null));

        if (submission == null) {
            submission = Submission.builder()
                    .exam(exam)
                    .student(student)
                    .attemptNo(1)
                    .versionCode(sheet.getDetectedExamCode())
                    .startTime(sheet.getUploadTime())
                    .build();
        }

        submission.setVersionCode(sheet.getDetectedExamCode());
        submission.setScore(outcome.score());
        submission.setSubmitTime(LocalDateTime.now());
        if (submission.getStartTime() == null) {
            submission.setStartTime(sheet.getUploadTime());
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
                    OmrBubbleResponse bubble = null;
                    if (d.getBubbleJson() != null) {
                        try {
                            var b = objectMapper.readValue(d.getBubbleJson(), OmrScanResponse.OmrBubble.class);
                            bubble = OmrBubbleResponse.builder()
                                    .choice(b.getChoice())
                                    .x(b.getX())
                                    .y(b.getY())
                                    .w(b.getW())
                                    .h(b.getH())
                                    .build();
                        } catch (JsonProcessingException ignored) {
                        }
                    }
                    return OmrAnswerItemResponse.builder()
                            .question(d.getQuestionNumber())
                            .chosen(d.getChosen())
                            .correctAnswer(d.getCorrectAnswer())
                            .isCorrect(d.getIsCorrect())
                            .status(d.getOmrStatus())
                            .bubble(bubble)
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
                .originalImageUrl(sheet.getFilePath())
                .needReview(needReview)
                .answers(answers)
                .gradedAt(sheet.getGradedAt())
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
