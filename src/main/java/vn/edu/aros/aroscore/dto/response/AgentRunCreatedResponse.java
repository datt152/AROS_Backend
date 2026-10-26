package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.AgentRunStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunCreatedResponse {
    private Long runId;
    private AgentRunStatus status;
    private String message;
}
