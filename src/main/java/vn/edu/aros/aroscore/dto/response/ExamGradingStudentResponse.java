package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamGradingStudentResponse {
    private Long studentId;
    private String fullName;
    private String email;
    private String studentCode;
    private GradingStatus status;
    private Long submissionId;
    private Double score;
    private String versionCode;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
