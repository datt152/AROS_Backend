package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSubmissionItemResponse {
    private Long submissionId;
    private Long examId;
    private String examTitle;
    private ExamPurpose purpose;
    private String versionCode;
    private Integer attemptNo;
    private GradingStatus status;
    private Double score;
    private Double maxScore;
    private Boolean scoreVisible;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
