package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardResponse {
    private Instant generatedAt;
    private TeacherDashboardStatsResponse stats;
    @Builder.Default
    private List<TeacherDashboardTodoResponse> todos = new ArrayList<>();
    @Builder.Default
    private List<TeacherDashboardActivityResponse> activities = new ArrayList<>();
    @Builder.Default
    private List<TeacherDashboardCalendarEventResponse> calendarEvents = new ArrayList<>();
}
