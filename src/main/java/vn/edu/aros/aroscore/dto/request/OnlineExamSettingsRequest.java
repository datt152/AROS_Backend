package vn.edu.aros.aroscore.dto.request;

import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;

@Data
public class OnlineExamSettingsRequest {
    private ExamStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean allowEdit;
    private Boolean showScoreToStudent;
    private Integer maxAttempts;
    private Boolean timeLimitEnabled;
}
