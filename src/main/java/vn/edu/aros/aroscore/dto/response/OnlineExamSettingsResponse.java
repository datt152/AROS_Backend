package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineExamSettingsResponse {
    private Long id;
    private ExamStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean allowEdit;
    private Boolean showScoreToStudent;
    private Integer maxAttempts;
    private Boolean timeLimitEnabled;
}
