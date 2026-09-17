package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardCalendarEventResponse {
    private Long id;
    private String title;
    /** ONLINE | PRACTICE — không OMR trên dashboard SV */
    private String kind;
    /** UPCOMING | ONGOING | ENDING_SOON | CLOSED */
    private String phase;
    private LocalDate date;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private Long subjectId;
    private String subjectName;
    private Long classroomId;
    private int classroomCount;
    private ExamStatus status;
    private GradingStatus myStatus;
}
