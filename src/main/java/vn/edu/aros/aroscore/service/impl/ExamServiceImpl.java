package vn.edu.aros.aroscore.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.matrix.AnswerMapping;
import vn.edu.aros.aroscore.dto.matrix.QuestionMatrix;
import vn.edu.aros.aroscore.dto.request.AssignExamClassroomsRequest;
import vn.edu.aros.aroscore.dto.request.ExamConfigRequest;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamUpdateRequest;
import vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest;
import vn.edu.aros.aroscore.dto.response.*;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.ExamType;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.mapper.ExamMapper;
import vn.edu.aros.aroscore.repository.*;
import vn.edu.aros.aroscore.service.ExamService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private static final String[] LABELS = {"A", "B", "C", "D", "E", "F", "G", "H"};

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamMapper examMapper;
    private final ExamVersionRepository examVersionRepository;
    private final SubmissionRepository submissionRepository;
    private final ClassroomRepository classroomRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Exam getOwnedExam(Long examId) {
        return examRepository.findByIdAndTeacherEmail(examId, getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi hoặc bạn không có quyền!"));
    }

    private ExamResponse toFullResponse(Exam exam) {
        ExamResponse response = examMapper.toResponse(exam);
        if (exam.getClassrooms() != null) {
            response.setClassroomIds(exam.getClassrooms().stream()
                    .map(Classroom::getId)
                    .toList());
        } else {
            response.setClassroomIds(List.of());
        }
        if (exam.getConfig() != null) {
            response.setConfig(toConfigResponse(exam.getConfig()));
        }
        return response;
    }

    private ExamConfigResponse toConfigResponse(ExamConfig config) {
        return ExamConfigResponse.builder()
                .id(config.getId())
                .semester(config.getSemester())
                .academicYear(config.getAcademicYear())
                .totalQuestions(config.getTotalQuestions())
                .examType(config.getExamType())
                .shuffleQuestions(config.getShuffleQuestions())
                .shuffleAnswers(config.getShuffleAnswers())
                .paperCount(config.getPaperCount())
                .allowEdit(config.getAllowEdit())
                .showScoreToStudent(config.getShowScoreToStudent())
                .maxAttempts(config.getMaxAttempts())
                .timeLimitEnabled(config.getTimeLimitEnabled())
                .build();
    }

    private ExamType toExamType(ExamMode mode) {
        return mode == ExamMode.OMR_PAPER ? ExamType.OMR : ExamType.ONLINE;
    }

    private ExamConfig buildDefaultConfig(Exam exam, ExamConfigRequest request, int questionCount) {
        boolean practice = exam.getPurpose() == ExamPurpose.PRACTICE;
        ExamConfig config = ExamConfig.builder()
                .exam(exam)
                .totalQuestions(questionCount)
                .examType(toExamType(exam.getExamMode()))
                .shuffleQuestions(true)
                .shuffleAnswers(true)
                .paperCount(1)
                .allowEdit(true)
                .showScoreToStudent(true)
                .maxAttempts(practice ? null : 1)
                .timeLimitEnabled(!practice)
                .build();
        applyConfigRequest(config, request, questionCount, exam.getExamMode());
        return config;
    }

    private void applyConfigRequest(ExamConfig config, ExamConfigRequest request, int questionCount, ExamMode mode) {
        config.setTotalQuestions(questionCount);
        config.setExamType(toExamType(mode));
        if (request == null) {
            return;
        }
        if (request.getSemester() != null) {
            config.setSemester(request.getSemester());
        }
        if (request.getAcademicYear() != null) {
            config.setAcademicYear(request.getAcademicYear());
        }
        if (request.getShuffleQuestions() != null) {
            config.setShuffleQuestions(request.getShuffleQuestions());
        }
        if (request.getShuffleAnswers() != null) {
            config.setShuffleAnswers(request.getShuffleAnswers());
        }
        if (request.getPaperCount() != null) {
            config.setPaperCount(request.getPaperCount());
        }
        if (request.getAllowEdit() != null) {
            config.setAllowEdit(request.getAllowEdit());
        }
        if (request.getShowScoreToStudent() != null) {
            config.setShowScoreToStudent(request.getShowScoreToStudent());
        }
        if (request.getMaxAttempts() != null) {
            if (request.getMaxAttempts() < 1) {
                throw new RuntimeException("Số lần làm bài phải >= 1!");
            }
            config.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getTimeLimitEnabled() != null) {
            config.setTimeLimitEnabled(request.getTimeLimitEnabled());
        }
    }

    private boolean isTimeLimitEnabled(Exam exam) {
        ExamConfig config = exam.getConfig();
        if (config == null || config.getTimeLimitEnabled() == null) {
            return exam.getPurpose() != ExamPurpose.PRACTICE;
        }
        return Boolean.TRUE.equals(config.getTimeLimitEnabled());
    }

    private Integer effectiveMaxAttempts(Exam exam) {
        if (exam.getPurpose() != ExamPurpose.PRACTICE) {
            return 1;
        }
        ExamConfig config = exam.getConfig();
        if (config == null) {
            return null;
        }
        return config.getMaxAttempts();
    }

    private Set<Classroom> resolveClassrooms(List<Long> classroomIds, Long subjectId, String email) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Classroom> classrooms = classroomRepository.findAllByIdsAndSubjectAndLecturer(
                classroomIds, subjectId, email);
        if (classrooms.size() != classroomIds.stream().distinct().count()) {
            throw new RuntimeException("Một số lớp không tồn tại, không thuộc môn này, hoặc bạn không có quyền!");
        }
        for (Classroom classroom : classrooms) {
            if (Boolean.FALSE.equals(classroom.getIsActive())) {
                throw new RuntimeException("Lớp \"" + classroom.getClassName() + "\" đã ngừng hoạt động!");
            }
        }
        return new HashSet<>(classrooms);
    }

    private void assertCanPublish(Exam exam, ExamStatus newStatus) {
        if (newStatus == ExamStatus.ONGOING || newStatus == ExamStatus.UPCOMING) {
            if (exam.getClassrooms() == null || exam.getClassrooms().isEmpty()) {
                throw new RuntimeException("Phải giao đề cho ít nhất 1 lớp trước khi mở bài thi!");
            }
            if (examVersionRepository.findByExamId(exam.getId()).isEmpty()) {
                throw new RuntimeException("Phải sinh mã đề trước khi mở bài thi!");
            }
        }
    }

    private void assertExamOpenForTaking(Exam exam) {
        if (exam.getStatus() == ExamStatus.CLOSED || exam.getStatus() == ExamStatus.COMPLETED) {
            throw new RuntimeException("Bài thi đã kết thúc hoặc đã đóng!");
        }
        if (exam.getStatus() == ExamStatus.DRAFT) {
            throw new RuntimeException("Bài thi chưa được mở (DRAFT). Giáo viên cần đổi trạng thái trước khi làm bài!");
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartAt() != null && now.isBefore(exam.getStartAt())) {
            throw new RuntimeException("Bài thi chưa đến thời gian mở!");
        }
        if (exam.getEndAt() != null && now.isAfter(exam.getEndAt())) {
            throw new RuntimeException("Bài thi đã hết hạn!");
        }
    }

    private void assertStudentCanTake(Exam exam, User student) {
        if (exam.getClassrooms() == null || exam.getClassrooms().isEmpty()) {
            throw new RuntimeException("Đề thi chưa được giao cho lớp nào!");
        }
        Set<Long> classroomIds = exam.getClassrooms().stream()
                .map(Classroom::getId)
                .collect(Collectors.toSet());
        if (!classroomRepository.isStudentInAnyClassroom(classroomIds, student.getId())) {
            throw new RuntimeException("Bạn không thuộc lớp được giao đề thi này!");
        }
    }

    @Override
    @Transactional
    public ExamResponse createExam(ExamCreateRequest request) {
        String email = getCurrentUserEmail();
        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giáo viên!"));

        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền truy cập!"));

        List<Question> questions = questionRepository.findAllById(request.getQuestionIds());
        if (questions.size() != request.getQuestionIds().size()) {
            throw new RuntimeException("Một số câu hỏi không tồn tại trong hệ thống!");
        }

        for (Question q : questions) {
            if (Boolean.FALSE.equals(q.getIsActive())) {
                throw new RuntimeException("Câu hỏi ID " + q.getId() + " đã bị xóa khỏi ngân hàng, không thể thêm vào đề mới!");
            }
            if (!q.getSubject().getId().equals(subject.getId())) {
                throw new RuntimeException("Câu hỏi ID " + q.getId() + " không thuộc môn học này!");
            }
            if (request.getExamMode() == ExamMode.OMR_PAPER && q.getType() == QuestionType.MULTIPLE_CHOICE) {
                throw new RuntimeException("LỖI: Đề thi OMR không được chứa câu hỏi nhiều đáp án (ID: " + q.getId() + ")");
            }
        }

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .duration(request.getDuration())
                .examMode(request.getExamMode())
                .purpose(request.getPurpose() != null ? request.getPurpose() : ExamPurpose.EXAM)
                .subject(subject)
                .teacher(teacher)
                .maxScore(request.getMaxScore())
                .status(ExamStatus.DRAFT)
                .classrooms(resolveClassrooms(request.getClassroomIds(), subject.getId(), email))
                .build();

        int order = 1;
        for (Long questionId : request.getQuestionIds()) {
            Question matchedQuestion = questions.stream()
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElseThrow();

            Double rawPoint = 1.0;
            if (request.getRawPoints() != null && request.getRawPoints().containsKey(questionId)) {
                rawPoint = request.getRawPoints().get(questionId);
            }

            exam.addQuestion(matchedQuestion, order, rawPoint);
            order++;
        }

        ExamConfig config = buildDefaultConfig(exam, request.getConfig(), request.getQuestionIds().size());
        exam.setConfig(config);

        return toFullResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public List<String> generateExamVersions(ExamVersionCreateRequest request) {
        Exam exam = getOwnedExam(request.getExamId());

        if (exam.getExamQuestions() == null || exam.getExamQuestions().isEmpty()) {
            throw new RuntimeException("Đề thi chưa có câu hỏi, không thể sinh mã đề!");
        }

        ExamConfig config = exam.getConfig();
        boolean shuffleQuestions = config == null || !Boolean.FALSE.equals(config.getShuffleQuestions());
        boolean shuffleAnswers = config == null || !Boolean.FALSE.equals(config.getShuffleAnswers());

        List<String> finalCodes = new ArrayList<>();
        if (request.getManualVersionCodes() != null && !request.getManualVersionCodes().isEmpty()) {
            finalCodes.addAll(request.getManualVersionCodes());
        } else if (request.getAutoGenerateCount() != null && request.getAutoGenerateCount() > 0) {
            int existingCount = examVersionRepository.findByExamId(exam.getId()).size();
            for (int i = 1; i <= request.getAutoGenerateCount(); i++) {
                finalCodes.add(String.format("%03d", existingCount + i));
            }
        } else if (config != null && config.getPaperCount() != null && config.getPaperCount() > 0
                && examVersionRepository.findByExamId(exam.getId()).isEmpty()) {
            for (int i = 1; i <= config.getPaperCount(); i++) {
                finalCodes.add(String.format("%03d", i));
            }
        } else {
            throw new RuntimeException("Bạn phải cung cấp danh sách mã đề hoặc số lượng đề cần tự sinh!");
        }

        boolean replaceExisting = Boolean.TRUE.equals(request.getReplaceExisting());
        List<String> duplicates = finalCodes.stream()
                .filter(code -> examVersionRepository.existsByExamIdAndVersionCode(exam.getId(), code))
                .distinct()
                .toList();

        if (!duplicates.isEmpty() && !replaceExisting) {
            throw new RuntimeException("Mã đề đã tồn tại: " + String.join(", ", duplicates)
                    + ". Gửi replaceExisting=true để ghi đè.");
        }

        if (replaceExisting && !duplicates.isEmpty()) {
            examVersionRepository.deleteByExamIdAndVersionCodeIn(exam.getId(), duplicates);
        }

        List<ExamVersion> savedVersions = new ArrayList<>();
        for (String code : finalCodes) {
            List<ExamQuestion> orderedQuestions = new ArrayList<>(exam.getExamQuestions());
            orderedQuestions.sort(Comparator.comparingInt(ExamQuestion::getQuestionOrder));
            if (shuffleQuestions) {
                java.util.Collections.shuffle(orderedQuestions);
            }

            List<QuestionMatrix> matrixList = new ArrayList<>();
            int newQuestionOrder = 1;

            for (ExamQuestion eq : orderedQuestions) {
                Question q = eq.getQuestion();
                List<AnswerOption> options = new ArrayList<>(q.getOptions());
                if (shuffleAnswers) {
                    java.util.Collections.shuffle(options);
                }

                if (options.size() > LABELS.length) {
                    throw new RuntimeException("Câu hỏi ID " + q.getId() + " có quá nhiều đáp án (tối đa "
                            + LABELS.length + ")!");
                }

                List<AnswerMapping> answerMappings = new ArrayList<>();
                for (int i = 0; i < options.size(); i++) {
                    AnswerOption opt = options.get(i);
                    answerMappings.add(new AnswerMapping(opt.getId(), LABELS[i], opt.getIsCorrect()));
                }

                matrixList.add(new QuestionMatrix(q.getId(), newQuestionOrder, answerMappings));
                newQuestionOrder++;
            }

            try {
                String jsonMatrix = objectMapper.writeValueAsString(matrixList);
                savedVersions.add(ExamVersion.builder()
                        .exam(exam)
                        .versionCode(code)
                        .shuffleMatrix(jsonMatrix)
                        .build());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Lỗi hệ thống khi sinh ma trận hoán vị JSON", e);
            }
        }

        examVersionRepository.saveAll(savedVersions);
        return finalCodes;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamVersionDetailResponse getExamVersionDetail(Long examId, String versionCode)
            throws JsonProcessingException {
        getOwnedExam(examId);

        ExamVersion version = examVersionRepository.findByExamIdAndVersionCode(examId, versionCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã đề " + versionCode + " của kỳ thi này!"));

        Exam exam = version.getExam();
        List<QuestionMatrix> matrixList = objectMapper.readValue(
                version.getShuffleMatrix(),
                new TypeReference<List<QuestionMatrix>>() {});
        matrixList.sort(Comparator.comparingInt(QuestionMatrix::getNewOrder));

        List<QuestionInVersionResponse> questionResponses = new ArrayList<>();
        for (QuestionMatrix qMatrix : matrixList) {
            ExamQuestion originalEq = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(qMatrix.getOriginalQuestionId()))
                    .findFirst()
                    .orElseThrow();

            Question originalQ = originalEq.getQuestion();
            List<OptionInVersionResponse> optionResponses = new ArrayList<>();
            for (AnswerMapping aMap : qMatrix.getAnswerMappings()) {
                AnswerOption originalOpt = originalQ.getOptions().stream()
                        .filter(opt -> opt.getId().equals(aMap.getOriginalOptionId()))
                        .findFirst()
                        .orElseThrow();

                OptionInVersionResponse optRes = new OptionInVersionResponse();
                optRes.setLabel(aMap.getNewLabel());
                optRes.setContent(originalOpt.getContent());
                optionResponses.add(optRes);
            }
            optionResponses.sort(Comparator.comparing(OptionInVersionResponse::getLabel));

            QuestionInVersionResponse qRes = new QuestionInVersionResponse();
            qRes.setOriginalQuestionId(originalQ.getId());
            qRes.setContent(originalQ.getContent());
            qRes.setType(originalQ.getType());
            qRes.setOptions(optionResponses);
            questionResponses.add(qRes);
        }

        ExamVersionDetailResponse response = new ExamVersionDetailResponse();
        response.setExamId(exam.getId());
        response.setTitle(exam.getTitle());
        response.setDuration(exam.getDuration());
        response.setVersionCode(version.getVersionCode());
        response.setQuestions(questionResponses);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamResponse> getAllExams(Long classroomId, ExamPurpose purpose, Pageable pageable) {
        String email = getCurrentUserEmail();

        if (classroomId != null) {
            if (!classroomRepository.existsByIdAndLecturerEmail(classroomId, email)) {
                throw new RuntimeException("Không tìm thấy lớp học hoặc bạn không có quyền truy cập!");
            }
            if (purpose != null) {
                return examRepository.findAllByClassroomIdAndTeacherEmailAndPurpose(
                                classroomId, email, purpose, pageable)
                        .map(this::toFullResponse);
            }
            return examRepository.findAllByClassroomIdAndTeacherEmail(classroomId, email, pageable)
                    .map(this::toFullResponse);
        }

        if (purpose != null) {
            return examRepository.findAllByTeacherEmailAndPurpose(email, purpose, pageable)
                    .map(this::toFullResponse);
        }
        return examRepository.findAllByTeacherEmail(email, pageable)
                .map(this::toFullResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamGradingResponse getExamGrading(Long examId, Long classroomId) {
        String email = getCurrentUserEmail();
        Exam exam = getOwnedExam(examId);

        if (!classroomRepository.existsByIdAndLecturerEmail(classroomId, email)) {
            throw new RuntimeException("Không tìm thấy lớp học hoặc bạn không có quyền truy cập!");
        }

        boolean assigned = exam.getClassrooms() != null
                && exam.getClassrooms().stream().anyMatch(c -> c.getId().equals(classroomId));
        if (!assigned) {
            throw new RuntimeException("Đề thi chưa được giao cho lớp này!");
        }

        Classroom classroom = classroomRepository.findByIdWithStudents(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        Map<Long, Submission> byStudentId = new HashMap<>();
        for (Submission submission : submissionRepository.findAllByExamIdWithStudent(examId)) {
            putPreferredSubmission(byStudentId, submission);
        }

        List<User> students = classroom.getStudents() == null
                ? Collections.emptyList()
                : classroom.getStudents().stream()
                .sorted(Comparator
                        .comparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(User::getStudentCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        boolean timeLimited = isTimeLimitEnabled(exam);
        List<ExamGradingStudentResponse> rows = students.stream()
                .map(student -> toGradingStudentRow(
                        student, byStudentId.get(student.getId()), exam.getDuration(), timeLimited))
                .toList();

        return ExamGradingResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .classroomId(classroom.getId())
                .classroomName(classroom.getClassName())
                .maxScore(exam.getMaxScore())
                .students(rows)
                .build();
    }

    private ExamGradingStudentResponse toGradingStudentRow(
            User student, Submission submission, Integer durationMinutes, boolean timeLimitEnabled) {
        if (submission == null) {
            return ExamGradingStudentResponse.builder()
                    .studentId(student.getId())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .studentCode(student.getStudentCode())
                    .status(GradingStatus.NOT_STARTED)
                    .build();
        }

        GradingStatus status;
        if (submission.getSubmitTime() != null) {
            status = GradingStatus.SUBMITTED;
        } else if (timeLimitEnabled && isPastExamDuration(submission.getStartTime(), durationMinutes)) {
            status = GradingStatus.EXPIRED;
        } else {
            status = GradingStatus.IN_PROGRESS;
        }

        return ExamGradingStudentResponse.builder()
                .studentId(student.getId())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .studentCode(student.getStudentCode())
                .status(status)
                .submissionId(submission.getId())
                .score(submission.getScore())
                .versionCode(submission.getVersionCode())
                .startTime(submission.getStartTime())
                .submitTime(submission.getSubmitTime())
                .build();
    }

    private void putPreferredSubmission(Map<Long, Submission> byStudentId, Submission submission) {
        Long studentId = submission.getStudent().getId();
        Submission existing = byStudentId.get(studentId);
        if (existing == null || preferSubmission(submission, existing)) {
            byStudentId.put(studentId, submission);
        }
    }

    /** Ưu tiên bài đã nộp mới nhất; nếu cùng loại thì attemptNo cao hơn. */
    private boolean preferSubmission(Submission candidate, Submission current) {
        boolean candidateSubmitted = candidate.getSubmitTime() != null;
        boolean currentSubmitted = current.getSubmitTime() != null;
        if (candidateSubmitted != currentSubmitted) {
            return candidateSubmitted;
        }
        int candidateAttempt = candidate.getAttemptNo() != null ? candidate.getAttemptNo() : 1;
        int currentAttempt = current.getAttemptNo() != null ? current.getAttemptNo() : 1;
        return candidateAttempt >= currentAttempt;
    }

    private boolean isPastExamDuration(LocalDateTime startTime, Integer durationMinutes) {
        if (startTime == null || durationMinutes == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(startTime.plusMinutes(durationMinutes));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamStatsResponse getExamStats(Long examId, Long classroomId) {
        String email = getCurrentUserEmail();
        Exam exam = getOwnedExam(examId);

        Long responseClassroomId = null;
        String responseClassroomName = null;
        Set<Long> scopedStudentIds = new HashSet<>();

        if (classroomId != null) {
            if (!classroomRepository.existsByIdAndLecturerEmail(classroomId, email)) {
                throw new RuntimeException("Không tìm thấy lớp học hoặc bạn không có quyền truy cập!");
            }
            boolean assigned = exam.getClassrooms() != null
                    && exam.getClassrooms().stream().anyMatch(c -> c.getId().equals(classroomId));
            if (!assigned) {
                throw new RuntimeException("Đề thi chưa được giao cho lớp này!");
            }

            Classroom classroom = classroomRepository.findByIdWithStudents(classroomId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));
            responseClassroomId = classroom.getId();
            responseClassroomName = classroom.getClassName();
            if (classroom.getStudents() != null) {
                classroom.getStudents().forEach(s -> scopedStudentIds.add(s.getId()));
            }
        } else {
            Set<Long> classroomIds = exam.getClassrooms() == null
                    ? Collections.emptySet()
                    : exam.getClassrooms().stream().map(Classroom::getId).collect(Collectors.toSet());
            if (!classroomIds.isEmpty()) {
                for (Classroom classroom : classroomRepository.findAllByIdInWithStudents(classroomIds)) {
                    if (classroom.getStudents() != null) {
                        classroom.getStudents().forEach(s -> scopedStudentIds.add(s.getId()));
                    }
                }
            }
        }

        Map<Long, Submission> byStudentId = new HashMap<>();
        for (Submission submission : submissionRepository.findAllByExamIdWithStudent(examId)) {
            putPreferredSubmission(byStudentId, submission);
        }

        int submitted = 0;
        int inProgress = 0;
        int expired = 0;
        int notStarted = 0;
        List<Double> scores = new ArrayList<>();
        boolean timeLimited = isTimeLimitEnabled(exam);

        for (Long studentId : scopedStudentIds) {
            Submission submission = byStudentId.get(studentId);
            if (submission == null) {
                notStarted++;
                continue;
            }
            if (submission.getSubmitTime() != null) {
                submitted++;
                if (submission.getScore() != null) {
                    scores.add(submission.getScore());
                }
            } else if (timeLimited && isPastExamDuration(submission.getStartTime(), exam.getDuration())) {
                expired++;
            } else {
                inProgress++;
            }
        }

        Double average = null;
        Double highest = null;
        Double lowest = null;
        if (!scores.isEmpty()) {
            average = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            highest = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            lowest = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            average = Math.round(average * 100.0) / 100.0;
        }

        List<ScoreBucketResponse> distribution = buildScoreDistribution(scores);
        List<QuestionStatsResponse> questionStats = buildQuestionStats(exam, scopedStudentIds);

        return ExamStatsResponse.builder()
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .maxScore(exam.getMaxScore())
                .classroomId(responseClassroomId)
                .classroomName(responseClassroomName)
                .totalStudents(scopedStudentIds.size())
                .submittedCount(submitted)
                .inProgressCount(inProgress)
                .expiredCount(expired)
                .notStartedCount(notStarted)
                .averageScore(average)
                .highestScore(highest)
                .lowestScore(lowest)
                .scoreDistribution(distribution)
                .questionStats(questionStats)
                .build();
    }

    private List<ScoreBucketResponse> buildScoreDistribution(List<Double> scores) {
        int[] counts = new int[10];
        for (Double score : scores) {
            if (score == null) {
                continue;
            }
            int bucket;
            if (score >= 10.0) {
                bucket = 9;
            } else if (score < 0) {
                bucket = 0;
            } else {
                bucket = (int) Math.floor(score);
                if (bucket > 9) {
                    bucket = 9;
                }
            }
            counts[bucket]++;
        }

        List<ScoreBucketResponse> buckets = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            double from = i;
            double to = i + 1.0;
            buckets.add(ScoreBucketResponse.builder()
                    .label(((int) from) + "–" + ((int) to))
                    .minInclusive(from)
                    .maxExclusive(i == 9 ? 10.0001 : to)
                    .count(counts[i])
                    .build());
        }
        return buckets;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<QuestionStatsResponse> buildQuestionStats(Exam exam, Set<Long> scopedStudentIds) {
        List<ExamQuestion> examQuestions = exam.getExamQuestions() == null
                ? Collections.emptyList()
                : exam.getExamQuestions().stream()
                .sorted(Comparator.comparing(ExamQuestion::getQuestionOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<Submission> submitted = submissionRepository.findSubmittedWithDetailsByExamId(exam.getId());
        if (!scopedStudentIds.isEmpty()) {
            submitted = submitted.stream()
                    .filter(s -> scopedStudentIds.contains(s.getStudent().getId()))
                    .toList();
        } else {
            submitted = Collections.emptyList();
        }

        Map<Long, int[]> counters = new HashMap<>();
        for (ExamQuestion eq : examQuestions) {
            counters.put(eq.getQuestion().getId(), new int[]{0, 0});
        }

        for (Submission submission : submitted) {
            if (submission.getDetails() == null) {
                continue;
            }
            for (SubmissionDetail detail : submission.getDetails()) {
                Long questionId = detail.getQuestion().getId();
                int[] counter = counters.get(questionId);
                if (counter == null) {
                    continue;
                }
                counter[0]++;
                if (Boolean.TRUE.equals(detail.getIsCorrect())) {
                    counter[1]++;
                }
            }
        }

        List<QuestionStatsResponse> result = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Long questionId = eq.getQuestion().getId();
            int[] counter = counters.getOrDefault(questionId, new int[]{0, 0});
            int answered = counter[0];
            int correct = counter[1];
            Double rate = answered == 0 ? null : round2((double) correct / answered);

            result.add(QuestionStatsResponse.builder()
                    .questionId(questionId)
                    .order(eq.getQuestionOrder())
                    .content(eq.getQuestion().getContent())
                    .rawPoint(eq.getRawPoint())
                    .answeredCount(answered)
                    .correctCount(correct)
                    .correctRate(rate)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long id) {
        return toFullResponse(getOwnedExam(id));
    }

    @Override
    @Transactional
    public ExamResponse updateExam(Long id, ExamUpdateRequest request) {
        String email = getCurrentUserEmail();
        Exam exam = getOwnedExam(id);

        if (exam.getConfig() != null && Boolean.FALSE.equals(exam.getConfig().getAllowEdit())
                && submissionRepository.existsByExamAndSubmitTimeIsNotNull(exam)) {
            throw new RuntimeException("Đề thi không cho phép chỉnh sửa sau khi đã có bài nộp!");
        }

        exam.setTitle(request.getTitle());
        exam.setDuration(request.getDuration());
        exam.setExamMode(request.getExamMode());
        if (request.getPurpose() != null) {
            exam.setPurpose(request.getPurpose());
        }
        exam.setMaxScore(request.getMaxScore());
        exam.setStartAt(request.getStartAt());
        exam.setEndAt(request.getEndAt());

        if (request.getStatus() != null) {
            assertCanPublish(exam, request.getStatus());
            exam.setStatus(request.getStatus());
        }

        if (!exam.getSubject().getId().equals(request.getSubjectId())) {
            Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                    .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

            boolean allMatchSubject = exam.getExamQuestions().stream()
                    .allMatch(eq -> eq.getQuestion().getSubject().getId().equals(subject.getId()));
            if (!allMatchSubject) {
                throw new RuntimeException("Không thể đổi môn: đề đang chứa câu hỏi không thuộc môn mới!");
            }
            exam.setSubject(subject);
        }

        if (exam.getStartAt() != null && exam.getEndAt() != null && exam.getEndAt().isBefore(exam.getStartAt())) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        }

        int questionCount = exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0;
        if (exam.getConfig() == null) {
            exam.setConfig(buildDefaultConfig(exam, request.getConfig(), questionCount));
        } else {
            applyConfigRequest(exam.getConfig(), request.getConfig(), questionCount, exam.getExamMode());
        }

        return toFullResponse(examRepository.save(exam));
    }

    @Override
    @Transactional
    public void deleteExam(Long id) {
        Exam exam = getOwnedExam(id);

        if (submissionRepository.existsByExamAndSubmitTimeIsNotNull(exam)) {
            throw new RuntimeException("Đề thi đã có bài nộp, không thể xóa để bảo toàn dữ liệu điểm!");
        }

        submissionRepository.deleteByExam(exam);
        examRepository.delete(exam);
    }

    @Override
    @Transactional
    public ExamResponse assignClassrooms(Long examId, AssignExamClassroomsRequest request) {
        String email = getCurrentUserEmail();
        Exam exam = getOwnedExam(examId);
        Set<Classroom> classrooms = resolveClassrooms(
                request.getClassroomIds(), exam.getSubject().getId(), email);
        exam.setClassrooms(classrooms);
        return toFullResponse(examRepository.save(exam));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomResponse> getAssignedClassrooms(Long examId) {
        Exam exam = getOwnedExam(examId);
        if (exam.getClassrooms() == null || exam.getClassrooms().isEmpty()) {
            return List.of();
        }
        return exam.getClassrooms().stream()
                .map(c -> ClassroomResponse.builder()
                        .id(c.getId())
                        .className(c.getClassName())
                        .description(c.getDescription())
                        .semester(c.getSemester())
                        .academicYear(c.getAcademicYear())
                        .isActive(c.getIsActive())
                        .subjectId(c.getSubject().getId())
                        .subjectName(c.getSubject().getSubjectName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listVersionCodes(Long examId) {
        getOwnedExam(examId);
        return examVersionRepository.findByExamId(examId).stream()
                .map(ExamVersion::getVersionCode)
                .sorted()
                .toList();
    }

    @Override
    @Transactional
    @SneakyThrows
    public ExamTakeResponse takeExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi với ID: " + examId));

        assertExamOpenForTaking(exam);

        User student = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));

        assertStudentCanTake(exam, student);

        List<ExamVersion> versions = examVersionRepository.findByExamId(examId);
        if (versions.isEmpty()) {
            throw new RuntimeException("Bài thi chưa được tạo mã đề!");
        }

        Submission draft = submissionRepository.findByExamAndStudentAndSubmitTimeIsNull(exam, student)
                .orElse(null);
        ExamVersion assignedVersion;

        if (draft != null && draft.getVersionCode() != null) {
            assignedVersion = examVersionRepository
                    .findByExamIdAndVersionCode(examId, draft.getVersionCode())
                    .orElseThrow(() -> new RuntimeException("Mã đề đã gán không còn tồn tại!"));
        } else {
            long submittedCount = submissionRepository.countByExamAndStudentAndSubmitTimeIsNotNull(exam, student);
            Integer maxAttempts = effectiveMaxAttempts(exam);
            if (maxAttempts != null && submittedCount >= maxAttempts) {
                throw new RuntimeException(exam.getPurpose() == ExamPurpose.PRACTICE
                        ? "Bạn đã hết số lần làm bài luyện tập!"
                        : "Bạn đã nộp bài thi này rồi!");
            }

            assignedVersion = versions.get(new java.util.Random().nextInt(versions.size()));
            int nextAttempt = submissionRepository.findMaxAttemptNo(exam, student) + 1;
            draft = Submission.builder()
                    .exam(exam)
                    .student(student)
                    .attemptNo(nextAttempt)
                    .versionCode(assignedVersion.getVersionCode())
                    .startTime(LocalDateTime.now())
                    .build();
            submissionRepository.save(draft);
        }

        if (isTimeLimitEnabled(exam) && draft.getStartTime() != null) {
            LocalDateTime deadline = draft.getStartTime().plusMinutes(exam.getDuration());
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new RuntimeException("Đã hết thời gian làm bài!");
            }
        }

        List<QuestionMatrix> matrixList = objectMapper.readValue(
                assignedVersion.getShuffleMatrix(),
                new TypeReference<List<QuestionMatrix>>() {});
        matrixList.sort(Comparator.comparingInt(QuestionMatrix::getNewOrder));

        List<QuestionTakeResponse> questionDTOs = new ArrayList<>();
        for (QuestionMatrix qm : matrixList) {
            Question originalQ = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(qm.getOriginalQuestionId()))
                    .findFirst()
                    .map(ExamQuestion::getQuestion)
                    .orElseThrow(() -> new RuntimeException("Lỗi đồng bộ dữ liệu câu hỏi"));

            List<OptionTakeResponse> optionDTOs = qm.getAnswerMappings().stream()
                    .map(mapping -> {
                        String answerContent = originalQ.getOptions().stream()
                                .filter(a -> a.getId().equals(mapping.getOriginalOptionId()))
                                .findFirst()
                                .map(AnswerOption::getContent)
                                .orElse("");
                        return OptionTakeResponse.builder()
                                .label(mapping.getNewLabel())
                                .content(answerContent)
                                .build();
                    })
                    .collect(Collectors.toList());
            optionDTOs.sort(Comparator.comparing(OptionTakeResponse::getLabel));

            questionDTOs.add(QuestionTakeResponse.builder()
                    .questionId(originalQ.getId())
                    .content(originalQ.getContent())
                    .type(originalQ.getType())
                    .options(optionDTOs)
                    .build());
        }

        ExamConfig config = exam.getConfig();
        return ExamTakeResponse.builder()
                .examId(exam.getId())
                .title(exam.getTitle())
                .purpose(exam.getPurpose())
                .duration(exam.getDuration())
                .timeLimitEnabled(isTimeLimitEnabled(exam))
                .showScoreToStudent(config == null || !Boolean.FALSE.equals(config.getShowScoreToStudent()))
                .attemptNo(draft.getAttemptNo())
                .maxAttempts(effectiveMaxAttempts(exam))
                .versionCode(assignedVersion.getVersionCode())
                .startTime(draft.getStartTime())
                .questions(questionDTOs)
                .build();
    }
}
