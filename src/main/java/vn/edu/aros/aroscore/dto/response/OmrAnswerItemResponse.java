package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmrAnswerItemResponse {
    private Integer question;
    private String chosen;
    private String correctAnswer;
    private Boolean isCorrect;
    private String status;
    private OmrBubbleResponse bubble;
}
