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
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.ExamVersionRepository;
import vn.edu.aros.aroscore.repository.SubmissionRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.SubmissionService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ExamRepository examRepository;
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
        // 1. Lấy thông tin đề thi và học sinh
        ExamVersion version = examVersionRepository.findByExamIdAndVersionCode(request.getExamId(), request.getVersionCode())
                .orElseThrow(() -> new RuntimeException("Mã đề không hợp lệ"));
        Exam exam = version.getExam();

        User student = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh"));

        // [Chốt chặn 1]: Kiểm tra xem học sinh đã nộp bài này chưa
        if (submissionRepository.existsByExamAndStudent(exam, student)) {
            throw new RuntimeException("Bạn đã nộp bài thi này rồi, không thể nộp lại!");
        }

        // 2. Giải mã ma trận
        List<QuestionMatrix> matrixList = objectMapper.readValue(version.getShuffleMatrix(), new TypeReference<List<QuestionMatrix>>(){});

        double totalRawPoints = 0.0;
        double earnedRawPoints = 0.0;
        int correctCount = 0; // Biến đếm số câu đúng

        Submission submission = Submission.builder()
                .exam(exam)
                .student(student)
                .versionCode(request.getVersionCode())
                .startTime(LocalDateTime.now().minusMinutes(exam.getDuration())) // Demo thời gian
                .submitTime(LocalDateTime.now())
                .build();

        // 3. Chấm điểm từng câu
        for (QuestionMatrix qm : matrixList) {
            // Lấy điểm thô (trọng số) từ đề gốc
            Double rawPoint = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(qm.getOriginalQuestionId()))
                    .findFirst()
                    .map(ExamQuestion::getRawPoint)
                    .orElse(1.0);

            totalRawPoints += rawPoint;

            // Lấy đáp án học sinh chọn (có thể null nếu bỏ trống)
            String studentAnswer = request.getAnswers().get(qm.getOriginalQuestionId());
            boolean isCorrect = false;

            // Tìm đáp án đúng trong ma trận hoán vị
            List<String> correctLabels = qm.getAnswerMappings().stream()
                    .filter(am -> Boolean.TRUE.equals(am.getIsCorrect()))
                    .map(AnswerMapping::getNewLabel)
                    .toList();

            // [Chốt chặn 2]: Xử lý logic so sánh nếu học sinh có chọn đáp án
            if (studentAnswer != null && !studentAnswer.trim().isEmpty()) {
                // Tách mảng nếu có nhiều đáp án (ví dụ: "A,B")
                String[] studentChoices = studentAnswer.split(",");

                // Trả lời đúng nếu số lượng lựa chọn bằng nhau VÀ khớp toàn bộ nội dung
                if (correctLabels.size() == studentChoices.length &&
                        correctLabels.containsAll(Arrays.asList(studentChoices))) {
                    isCorrect = true;
                }
            }

            if (isCorrect) {
                earnedRawPoints += rawPoint;
                correctCount++;
            }

            // Lưu chi tiết
            submission.addDetail(SubmissionDetail.builder()
                    .question(exam.getExamQuestions().stream()
                            .filter(eq -> eq.getQuestion().getId().equals(qm.getOriginalQuestionId()))
                            .findFirst().get().getQuestion())
                    .selectedAnswer(studentAnswer) // Nếu bỏ trống thì lưu null
                    .isCorrect(isCorrect)
                    .build());
        }

        // 4. Tính điểm theo Hệ số chuẩn hóa
        double finalScore = (totalRawPoints > 0) ? (earnedRawPoints / totalRawPoints) * exam.getMaxScore() : 0;
        submission.setScore(finalScore);

        // 5. Lưu kết quả
        submissionRepository.save(submission);

        // 6. Trả về kết quả hoàn chỉnh
        return SubmissionResponse.builder()
                .submissionId(submission.getId())
                .totalScore(finalScore)
                .maxScore(exam.getMaxScore())
                .correctQuestions(correctCount)
                .totalQuestions(matrixList.size())
                .build();
    }
}