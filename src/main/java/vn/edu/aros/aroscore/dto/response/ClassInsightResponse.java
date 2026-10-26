package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassInsightResponse {
    private Long id;
    private Long examId;
    private Long classroomId;
    private String classroomName;
    private String reportJson;
    private Long sourceRunId;
    private LocalDateTime createdAt;
}
