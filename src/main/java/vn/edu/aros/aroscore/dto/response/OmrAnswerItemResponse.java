package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    /** ok | warning | error | empty */
    private String level;
    private String color;
    private OmrBubbleResponse bubble;
    /** Overlay ô đã tô: green = đúng, red = sai / tô trùng. Câu trống = []. */
    private List<OmrBubbleResponse> overlayBubbles;
}
