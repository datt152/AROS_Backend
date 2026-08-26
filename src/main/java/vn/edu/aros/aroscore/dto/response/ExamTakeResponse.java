package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;

import java.util.List;

@Data
@Builder
public class ExamTakeResponse {
    private Long examId;
    private String title;
    private ExamPurpose purpose;
    private Integer duration;
    private Boolean timeLimitEnabled;
    private Boolean showScoreToStudent;
    private Integer attemptNo;
    private Integer maxAttempts;
    private String versionCode;
    private java.time.LocalDateTime startTime;
    private List<QuestionTakeResponse> questions;
}
