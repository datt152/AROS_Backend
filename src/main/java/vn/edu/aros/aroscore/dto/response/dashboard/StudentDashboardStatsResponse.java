package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardStatsResponse {
    private long inProgressCount;
    private long upcomingExamCount;
    private long openPracticeCount;
    private long recentSubmittedCount;
}
