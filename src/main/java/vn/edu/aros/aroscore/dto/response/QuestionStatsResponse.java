package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatsResponse {
    private Long questionId;
    private Integer order;
    private String content;
    private Double rawPoint;
    private Integer answeredCount;
    private Integer correctCount;
    private Double correctRate;
}
