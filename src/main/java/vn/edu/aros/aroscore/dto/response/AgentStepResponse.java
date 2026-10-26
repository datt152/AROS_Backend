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
public class AgentStepResponse {
    private Long id;
    private Integer stepNo;
    private String thought;
    private String toolName;
    private String toolArgsJson;
    private String toolResultJson;
    private LocalDateTime createdAt;
}
