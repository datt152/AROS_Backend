package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardActivityResponse {
    private String id;
    private Instant occurredAt;
    private String type;
    private String message;
    private String entityType;
    private Long entityId;
}
