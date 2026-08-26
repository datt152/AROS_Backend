package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetailResponse {
    private Long submissionId;
    private Long examId;
    private String examTitle;
    private String versionCode;
    private Long studentId;
    private String fullName;
    private String email;
    private String studentCode;
    private GradingStatus status;
    private Double score;
    private Double maxScore;
    private Integer correctQuestions;
    private Integer totalQuestions;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private List<SubmissionDetailItemResponse> details;
}
