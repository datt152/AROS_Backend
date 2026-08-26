package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionResponse {
    private Long submissionId;
    private Integer attemptNo;
    private Double totalScore;
    private Double maxScore;
    private Integer correctQuestions;
    private Integer totalQuestions;
    private Boolean scoreVisible;
}
