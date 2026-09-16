package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamSessionStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionResponse {
    private Long id;
    private Long examId;
    private String examTitle;
    private Long classroomId;
    private String classroomName;
    private String name;
    private ExamSessionStatus status;
    private LocalDateTime createdAt;
    private Integer sheetCount;
}
