package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.AgentRunStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunResponse {
    private Long id;
    private Long examId;
    private Long classroomId;
    private String classroomName;
    private AgentRunStatus status;
    private String goal;
    private String finalReport;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private List<AgentStepResponse> steps;
}
