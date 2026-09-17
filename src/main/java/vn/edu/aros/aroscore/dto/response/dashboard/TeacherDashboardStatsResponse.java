package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardStatsResponse {
    private long classroomCount;
    private long subjectCount;
    private long onlineExamCount;
    private long onlineExamOngoingCount;
    private long onlineExamUpcomingCount;
    private long omrExamCount;
    private long omrActiveSessionCount;
    private long practiceCount;
    private long practiceOpenCount;
    private long questionCount;
    private long questionAddedLast7Days;
    private long templateCount;
    private long pendingActionCount;
}
