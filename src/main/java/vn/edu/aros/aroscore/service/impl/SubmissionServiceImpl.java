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
import vn.edu.aros.aroscore.dto.response.SubmissionDetailResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;
import vn.edu.aros.aroscore.repository.ExamVersionRepository;
import vn.edu.aros.aroscore.repository.SubmissionRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.SubmissionService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ExamVersionRepository examVersionRepository;
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

        if (exam.getStatus() == ExamStatus.CLOSED || exam.getStatus() == ExamStatus.COMPLETED) {
            throw new RuntimeException("Bài thi đã kết thúc hoặc đã đóng!");
        }

        User student = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));

        Submission submission = submissionRepository.findByExamAndStudentAndSubmitTimeIsNull(exam, student)
                .orElseThrow(() -> new RuntimeException(
                        "Bạn chưa bắt đầu bài thi. Hãy gọi API take trước khi nộp bài!"));

        if (!request.getVersionCode().equals(submission.getVersionCode())) {
            throw new RuntimeException("Mã đề nộp không khớp với mã đề đã được gán khi bắt đầu làm bài!");
        }

        ExamConfig config = exam.getConfig();
        boolean timeLimitEnabled = config == null
                ? exam.getPurpose() != ExamPurpose.PRACTICE
                : !Boolean.FALSE.equals(config.getTimeLimitEnabled());

        if (timeLimitEnabled && submission.getStartTime() != null) {
            LocalDateTime deadline = submission.getStartTime().plusMinutes(exam.getDuration());
            if (LocalDateTime.now().isAfter(deadline.plusMinutes(1))) {
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

        boolean scoreVisible = config == null || !Boolean.FALSE.equals(config.getShowScoreToStudent());

        return SubmissionResponse.builder()
                .submissionId(submission.getId())
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

        boolean scoreVisible = isTeacher
                || exam.getConfig() == null
                || !Boolean.FALSE.equals(exam.getConfig().getShowScoreToStudent());

        User student = submission.getStudent();
        GradingStatus status = resolveGradingStatus(submission, exam);

        Map<Long, Double> rawPointsByQuestionId = exam.getExamQuestions() == null
                ? Collections.emptyMap()
                : exam.getExamQuestions().stream()
                .collect(Collectors.toMap(
                        eq -> eq.getQuestion().getId(),
                        ExamQuestion::getRawPoint,
                        (a, b) -> a));

        Map<Long, QuestionMatrix> matrixByQuestionId = Collections.emptyMap();
        if (submission.getVersionCode() != null) {
            ExamVersion version = examVersionRepository
                    .findByExamIdAndVersionCode(exam.getId(), submission.getVersionCode())
                    .orElse(null);
            if (version != null && version.getShuffleMatrix() != null) {
                List<QuestionMatrix> matrixList = objectMapper.readValue(
                        version.getShuffleMatrix(),
                        new TypeReference<List<QuestionMatrix>>() {});
                matrixByQuestionId = matrixList.stream()
                        .collect(Collectors.toMap(QuestionMatrix::getOriginalQuestionId, m -> m, (a, b) -> a));
            }
        }

        List<SubmissionDetail> detailEntities = submission.getDetails() == null
                ? Collections.emptyList()
                : submission.getDetails();

        Map<Long, QuestionMatrix> finalMatrixByQuestionId = matrixByQuestionId;
        List<SubmissionDetailItemResponse> details;
        if (!scoreVisible && isOwner && !isTeacher) {
            details = List.of();
        } else {
            details = detailEntities.stream()
                    .map(detail -> {
                        Question question = detail.getQuestion();
                        QuestionMatrix matrix = finalMatrixByQuestionId.get(question.getId());
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
                                .selectedAnswer(detail.getSelectedAnswer())
                                .correctAnswer(scoreVisible ? correctAnswer : null)
                                .isCorrect(scoreVisible ? detail.getIsCorrect() : null)
                                .rawPoint(rawPointsByQuestionId.getOrDefault(question.getId(), 1.0))
                                .build();
                    })
                    .sorted(Comparator.comparing(
                            SubmissionDetailItemResponse::getOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .toList();
        }

        int correctCount = (int) detailEntities.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsCorrect()))
                .count();
        int totalQuestions = !detailEntities.isEmpty()
                ? detailEntities.size()
                : (exam.getExamQuestions() != null ? exam.getExamQuestions().size() : 0);

        return SubmissionDetailResponse.builder()
                .submissionId(submission.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
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
                .details(details)
                .build();
    }

    private StudentSubmissionItemResponse toStudentSubmissionItem(Submission submission) {
        Exam exam = submission.getExam();
        boolean scoreVisible = exam.getConfig() == null
                || !Boolean.FALSE.equals(exam.getConfig().getShowScoreToStudent());
        boolean submitted = submission.getSubmitTime() != null;

        return StudentSubmissionItemResponse.builder()
                .submissionId(submission.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
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
        ExamConfig config = exam.getConfig();
        boolean timeLimitEnabled = config == null
                ? exam.getPurpose() != ExamPurpose.PRACTICE
                : !Boolean.FALSE.equals(config.getTimeLimitEnabled());
        if (timeLimitEnabled
                && submission.getStartTime() != null
                && exam.getDuration() != null
                && LocalDateTime.now().isAfter(submission.getStartTime().plusMinutes(exam.getDuration()))) {
            return GradingStatus.EXPIRED;
        }
        return GradingStatus.IN_PROGRESS;
    }
}
