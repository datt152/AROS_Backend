package vn.edu.aros.aroscore.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardCalendarEventResponse {
    private Long id;
    private String title;
    private String kind;
    private String phase;
    private LocalDate date;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private Long subjectId;
    private String subjectName;
    private int classroomCount;
    private ExamStatus status;
}
