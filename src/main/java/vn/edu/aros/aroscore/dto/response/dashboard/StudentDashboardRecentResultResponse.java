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
public class StudentDashboardRecentResultResponse {
    private String id;
    private Instant occurredAt;
    /** SUBMITTED | GRADED */
    private String type;
    private String message;
    private String entityType;
    private Long entityId;
}
