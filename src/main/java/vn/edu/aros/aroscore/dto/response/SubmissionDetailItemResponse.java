package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetailItemResponse {
    private Long questionId;
    private Integer order;
    private String content;
    private QuestionType type;
    private String selectedAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Double rawPoint;
}
