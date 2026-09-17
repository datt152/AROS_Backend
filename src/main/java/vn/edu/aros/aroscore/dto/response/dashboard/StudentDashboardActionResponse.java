package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardActionResponse {
    private String id;
    /** CONTINUE_EXAM | CONTINUE_PRACTICE | UPCOMING_EXAM | OPEN_PRACTICE | VIEW_RESULT */
    private String type;
    private String priority;
    private String title;
    private String detail;
    private Long entityId;
    private Long classroomId;
    private String actionPath;
    private String actionLabel;
}
