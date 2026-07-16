package vn.edu.aros.aroscore.dto.response;

import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import java.time.LocalDateTime;

@Data
public class ExamResponse {
    private Long id;
    private String title;
    private Integer duration;
    private ExamMode examMode;
    private Long subjectId;
    private String teacherEmail;
    private LocalDateTime createdAt;
    private Integer totalQuestions;
}