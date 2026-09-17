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
public class StudentDashboardResponse {
    private Instant generatedAt;
    private StudentDashboardStatsResponse stats;
    @Builder.Default
    private List<StudentDashboardActionResponse> actions = new ArrayList<>();
    @Builder.Default
    private List<StudentDashboardRecentResultResponse> recentResults = new ArrayList<>();
    @Builder.Default
    private List<StudentDashboardCalendarEventResponse> calendarEvents = new ArrayList<>();
}
