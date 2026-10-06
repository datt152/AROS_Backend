package vn.edu.aros.aroscore.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.matrix.AnswerMapping;
import vn.edu.aros.aroscore.dto.matrix.QuestionMatrix;
import vn.edu.aros.aroscore.dto.request.SubmissionRequest;
import vn.edu.aros.aroscore.dto.response.StudentSubmissionItemResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionDetailItemResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionDetailOptionResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionDetailResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;
import vn.edu.aros.aroscore.service.exam.ExamSettingsSupport;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.repository.ExamVersionRepository;
import vn.edu.aros.aroscore.repository.SubmissionRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.SubmissionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ExamVersionRepository examVersionRepository;
    private final ClassroomRepository classroomRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    @SneakyThrows
    public SubmissionResponse submitExam(SubmissionRequest request) {
        ExamVersion version = examVersionRepository
                .findByExamIdAndVersionCode(request.getExamId(), request.getVersionCode())
                .orElseThrow(() -> new RuntimeException("Mã đề không hợp lệ"));
        Exam exam = version.getExam();

        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            throw new RuntimeException("Đề OMR không nộp online");
        }

        ExamStatus status = ExamSettingsSupport.onlineStatus(exam);
        if (status == ExamStatus.CLOSED || status == ExamStatus.COMPLETED) {
            throw new RuntimeException("Bài thi đã kết thúc hoặc đã đóng!");
        }

        User student = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));

        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học!"));
        boolean assigned = exam.getClassrooms() != null
                && exam.getClassrooms().stream().anyMatch(c -> c.getId().equals(classroom.getId()));
        if (!assigned) {
            throw new IllegalArgumentException("Lớp không thuộc đề thi này!");
        }

        Submission submission = submissionRepository
                .findByExamAndStudentAndClassroomAndSubmitTimeIsNull(exam, student, classroom)
                .orElseThrow(() -> new RuntimeException(
                        "Bạn chưa bắt đầu bài thi. Hãy gọi API take trước khi nộp bài!"));

        if (!request.getVersionCode().equals(submission.getVersionCode())) {
            throw new RuntimeException("Mã đề nộp không khớp với mã đề đã được gán khi bắt đầu làm bài!");
        }

        if (ExamSettingsSupport.isTimeLimitEnabled(exam) && submission.getStartTime() != null) {
            LocalDateTime deadline = submission.getStartTime().plusMinutes(exam.getDuration());
            if (LocalDateTime.now().isAfter(deadline.plusMinutes(3))) {
                throw new RuntimeException("Đã hết thời gian làm bài, không thể nộp!");
            }
        }

        List<QuestionMatrix> matrixList = objectMapper.readValue(
                version.getShuffleMatrix(),
                new TypeReference<List<QuestionMatrix>>() {});

        double totalRawPoints = 0.0;
        double earnedRawPoints = 0.0;
        int correctCount = 0;

        submission.getDetails().clear();

        for (QuestionMatrix qm : matrixList) {
            Double rawPoint = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(qm.getOriginalQuestionId()))
                    .findFirst()
                    .map(ExamQuestion::getRawPoint)
                    .orElse(1.0);

            totalRawPoints += rawPoint;

            String studentAnswer = request.getAnswers().get(qm.getOriginalQuestionId());
            boolean isCorrect = false;

            List<String> correctLabels = qm.getAnswerMappings().stream()
                    .filter(am -> Boolean.TRUE.equals(am.getIsCorrect()))
                    .map(AnswerMapping::getNewLabel)
                    .sorted()
                    .toList();

            if (studentAnswer != null && !studentAnswer.trim().isEmpty()) {
                String[] studentChoices = Arrays.stream(studentAnswer.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .sorted()
                        .toArray(String[]::new);

                if (correctLabels.size() == studentChoices.length
                        && correctLabels.containsAll(Arrays.asList(studentChoices))) {
                    isCorrect = true;
                }
            }

            if (isCorrect) {
                earnedRawPoints += rawPoint;
                correctCount++;
            }

            Question question = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(qm.getOriginalQuestionId()))
                    .findFirst()
                    .orElseThrow()
                    .getQuestion();

            submission.addDetail(SubmissionDetail.builder()
                    .question(question)
                    .selectedAnswer(studentAnswer)
                    .isCorrect(isCorrect)
                    .build());
        }

        double finalScore = (totalRawPoints > 0)
                ? (earnedRawPoints / totalRawPoints) * exam.getMaxScore()
                : 0;
        submission.setScore(finalScore);
        submission.setSubmitTime(LocalDateTime.now());

        submissionRepository.save(submission);

        boolean scoreVisible = ExamSettingsSupport.showScoreToStudent(exam);

        return SubmissionResponse.builder()
                .submissionId(submission.getId())
                .classroomId(classroom.getId())
                .attemptNo(submission.getAttemptNo())
                .totalScore(scoreVisible ? finalScore : null)
                .maxScore(scoreVisible ? exam.getMaxScore() : null)
                .correctQuestions(scoreVisible ? correctCount : null)
                .totalQuestions(matrixList.size())
                .scoreVisible(scoreVisible)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentSubmissionItemResponse> getMySubmissions(Long examId) {
        User student = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh!"));

        List<Submission> submissions = submissionRepository
                .findAllByStudentIdAndOptionalExamId(student.getId(), examId);

        return submissions.stream()
                .filter(s -> s.getExam() != null
                        && s.getExam().getExamMode() == ExamMode.ONLINE)
                .sorted(Comparator.comparing(Submission::getId).reversed())
                .map(this::toStudentSubmissionItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @SneakyThrows
    public SubmissionDetailResponse getSubmissionDetail(Long submissionId) {
        Submission submission = submissionRepository.findByIdWithDetails(submissionId)
                .orElseGet(() -> submissionRepository.findByIdWithStudentAndExam(submissionId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp!")));

        Exam exam = submission.getExam();
        String email = getCurrentUserEmail();
        boolean isTeacher = exam.getTeacher().getEmail().equals(email);
        boolean isOwner = submission.getStudent().getEmail().equals(email);
        if (!isTeacher && !isOwner) {
            throw new RuntimeException("Bạn không có quyền xem bài nộp này!");
        }

        boolean practice = exam.getPurpose() == ExamPurpose.PRACTICE;
        if (isOwner && !isTeacher && !practice) {
            throw new RuntimeException("Bài thi chính thức không xem lại chi tiết. Chỉ bài luyện tập được xem câu hỏi và đáp án.");
        }

        User student = submission.getStudent();
        GradingStatus status = resolveGradingStatus(submission, exam);
        boolean scoreVisible = isTeacher || practice || ExamSettingsSupport.showScoreToStudent(exam);
        boolean revealAnswers = isTeacher || (practice && status != GradingStatus.IN_PROGRESS);

        Map<Long, Double> rawPointsByQuestionId = exam.getExamQuestions() == null
                ? Collections.emptyMap()
                : exam.getExamQuestions().stream()
                .collect(Collectors.toMap(
                        eq -> eq.getQuestion().getId(),
                        ExamQuestion::getRawPoint,
                        (a, b) -> a));

        Map<Long, QuestionMatrix> parsedMatrixByQuestionId = Collections.emptyMap();
        List<QuestionMatrix> parsedMatrixList = Collections.emptyList();
        if (submission.getVersionCode() != null) {
            ExamVersion version = examVersionRepository
                    .findByExamIdAndVersionCode(exam.getId(), submission.getVersionCode())
                    .orElse(null);
            if (version != null && version.getShuffleMatrix() != null) {
                parsedMatrixList = objectMapper.readValue(
                        version.getShuffleMatrix(),
                        new TypeReference<List<QuestionMatrix>>() {});
                parsedMatrixByQuestionId = parsedMatrixList.stream()
                        .collect(Collectors.toMap(QuestionMatrix::getOriginalQuestionId, m -> m, (a, b) -> a));
            }
        }
        final Map<Long, QuestionMatrix> matrixByQuestionId = parsedMatrixByQuestionId;
        final List<QuestionMatrix> matrixList = parsedMatrixList;

        List<SubmissionDetail> detailEntities = submission.getDetails() == null
                ? Collections.emptyList()
                : submission.getDetails();
        Map<Long, String> selectedByQuestionId = detailEntities.stream()
                .filter(d -> d.getQuestion() != null)
                .collect(Collectors.toMap(
                        d -> d.getQuestion().getId(),
                        d -> d.getSelectedAnswer() == null ? "" : d.getSelectedAnswer(),
                        (a, b) -> a));
        Map<Long, Boolean> correctByQuestionId = detailEntities.stream()
                .filter(d -> d.getQuestion() != null)
                .collect(Collectors.toMap(
                        d -> d.getQuestion().getId(),
                        SubmissionDetail::getIsCorrect,
                        (a, b) -> a));

        List<SubmissionDetailItemResponse> details;
        if (!detailEntities.isEmpty()) {
            details = detailEntities.stream()
                    .map(detail -> toDetailItem(
                            detail.getQuestion(),
                            matrixByQuestionId.get(detail.getQuestion().getId()),
                            detail.getSelectedAnswer(),
                            detail.getIsCorrect(),
                            revealAnswers,
                            rawPointsByQuestionId))
                    .sorted(Comparator.comparing(
                            SubmissionDetailItemResponse::getOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        } else {
            Map<Long, Question> questionsById = exam.getExamQuestions() == null
                    ? Collections.emptyMap()
                    : exam.getExamQuestions().stream()
                    .collect(Collectors.toMap(eq -> eq.getQuestion().getId(), ExamQuestion::getQuestion, (a, b) -> a));
            details = matrixList.stream()
                    .map(matrix -> {
                        Question question = questionsById.get(matrix.getOriginalQuestionId());
                        if (question == null) {
                            return null;
                        }
                        String selected = selectedByQuestionId.get(question.getId());
                        return toDetailItem(
                                question,
                                matrix,
                                selected == null || selected.isBlank() ? null : selected,
                                correctByQuestionId.get(question.getId()),
                                revealAnswers,
                                rawPointsByQuestionId);
                    })
                    .filter(item -> item != null)
                    .sorted(Comparator.comparing(
                            SubmissionDetailItemResponse::getOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        }

        int correctCount = (int) detailEntities.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsCorrect()))
                .count();
        int totalQuestions = !details.isEmpty()
                ? details.size()
                : (exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0);

        return SubmissionDetailResponse.builder()
                .submissionId(submission.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .classroomId(submission.getClassroom() != null ? submission.getClassroom().getId() : null)
                .classroomName(submission.getClassroom() != null ? submission.getClassroom().getClassName() : null)
                .versionCode(submission.getVersionCode())
                .studentId(student.getId())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .studentCode(student.getStudentCode())
                .status(status)
                .score(scoreVisible ? submission.getScore() : null)
                .maxScore(scoreVisible ? exam.getMaxScore() : null)
                .correctQuestions(scoreVisible ? correctCount : null)
                .totalQuestions(totalQuestions)
                .startTime(submission.getStartTime())
                .submitTime(submission.getSubmitTime())
                .purpose(exam.getPurpose())
                .scoreVisible(scoreVisible)
                .details(details)
                .build();
    }

    private SubmissionDetailItemResponse toDetailItem(
            Question question,
            QuestionMatrix matrix,
            String selectedAnswer,
            Boolean isCorrect,
            boolean revealAnswers,
            Map<Long, Double> rawPointsByQuestionId) {
        Integer order = matrix != null ? matrix.getNewOrder() : null;
        String correctAnswer = matrix == null ? null : matrix.getAnswerMappings().stream()
                .filter(am -> Boolean.TRUE.equals(am.getIsCorrect()))
                .map(AnswerMapping::getNewLabel)
                .sorted()
                .collect(Collectors.joining(","));
        return SubmissionDetailItemResponse.builder()
                .questionId(question.getId())
                .order(order)
                .content(question.getContent())
                .type(question.getType())
                .selectedAnswer(selectedAnswer)
                .correctAnswer(revealAnswers ? correctAnswer : null)
                .isCorrect(revealAnswers ? isCorrect : null)
                .rawPoint(rawPointsByQuestionId.getOrDefault(question.getId(), 1.0))
                .options(buildDetailOptions(question, matrix, selectedAnswer, revealAnswers))
                .build();
    }

    private List<SubmissionDetailOptionResponse> buildDetailOptions(
            Question question,
            QuestionMatrix matrix,
            String selectedAnswer,
            boolean revealAnswers) {
        if (matrix == null || matrix.getAnswerMappings() == null) {
            return List.of();
        }
        Set<String> selected = new HashSet<>();
        if (selectedAnswer != null && !selectedAnswer.isBlank()) {
            Arrays.stream(selectedAnswer.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(selected::add);
        }
        List<SubmissionDetailOptionResponse> options = new ArrayList<>();
        for (AnswerMapping mapping : matrix.getAnswerMappings()) {
            String content = question.getOptions() == null
                    ? ""
                    : question.getOptions().stream()
                    .filter(option -> option.getId().equals(mapping.getOriginalOptionId()))
                    .map(AnswerOption::getContent)
                    .findFirst()
                    .orElse("");
            Boolean correct = Boolean.TRUE.equals(mapping.getIsCorrect());
            options.add(SubmissionDetailOptionResponse.builder()
                    .label(mapping.getNewLabel())
                    .content(content)
                    .selected(selected.contains(mapping.getNewLabel()))
                    .correct(revealAnswers ? correct : null)
                    .build());
        }
        options.sort(Comparator.comparing(
                SubmissionDetailOptionResponse::getLabel,
                Comparator.nullsLast(String::compareTo)));
        return options;
    }

    private StudentSubmissionItemResponse toStudentSubmissionItem(Submission submission) {
        Exam exam = submission.getExam();
        boolean scoreVisible = exam.getPurpose() == ExamPurpose.PRACTICE
                || ExamSettingsSupport.showScoreToStudent(exam);
        boolean submitted = submission.getSubmitTime() != null;

        return StudentSubmissionItemResponse.builder()
                .submissionId(submission.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .classroomId(submission.getClassroom() != null ? submission.getClassroom().getId() : null)
                .classroomName(submission.getClassroom() != null ? submission.getClassroom().getClassName() : null)
                .purpose(exam.getPurpose())
                .versionCode(submission.getVersionCode())
                .attemptNo(submission.getAttemptNo())
                .status(resolveGradingStatus(submission, exam))
                .score(submitted && scoreVisible ? submission.getScore() : null)
                .maxScore(scoreVisible ? exam.getMaxScore() : null)
                .scoreVisible(scoreVisible)
                .startTime(submission.getStartTime())
                .submitTime(submission.getSubmitTime())
                .build();
    }

    private GradingStatus resolveGradingStatus(Submission submission, Exam exam) {
        if (submission.getSubmitTime() != null) {
            return GradingStatus.SUBMITTED;
        }
        if (ExamSettingsSupport.isTimeLimitEnabled(exam)
                && submission.getStartTime() != null
                && exam.getDuration() != null
                && LocalDateTime.now().isAfter(submission.getStartTime().plusMinutes(exam.getDuration()))) {
            return GradingStatus.EXPIRED;
        }
        return GradingStatus.IN_PROGRESS;
    }
}
