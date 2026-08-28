package vn.edu.aros.aroscore.service.omr;

import org.springframework.stereotype.Component;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.ExamQuestion;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

import java.util.List;

@Component
public class OmrExamValidator {

    public static final int MAX_QUESTIONS = 100;
    public static final int CHOICES_PER_QUESTION = 4;

    public void validateOmrExam(Exam exam) {
        if (exam.getExamMode() != ExamMode.OMR_PAPER) {
            return;
        }
        List<ExamQuestion> questions = exam.getExamQuestions();
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("Đề OMR phải có ít nhất 1 câu hỏi!");
        }
        if (questions.size() > MAX_QUESTIONS) {
            throw new RuntimeException("Đề OMR tối đa " + MAX_QUESTIONS + " câu!");
        }
        for (ExamQuestion eq : questions) {
            var q = eq.getQuestion();
            if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                throw new RuntimeException("Đề OMR không hỗ trợ câu nhiều đáp án (ID: " + q.getId() + ")");
            }
            if (q.getOptions() == null || q.getOptions().size() != CHOICES_PER_QUESTION) {
                throw new RuntimeException("Câu ID " + q.getId() + " phải có đúng 4 đáp án (A-D)!");
            }
        }
    }
}
