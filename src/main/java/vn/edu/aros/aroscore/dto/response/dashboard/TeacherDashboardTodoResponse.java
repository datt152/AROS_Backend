package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardTodoResponse {
    private String id;
    private String type;
    private String priority;
    private String title;
    private String detail;
    private String entityType;
    private Long entityId;
    private String actionPath;
}
