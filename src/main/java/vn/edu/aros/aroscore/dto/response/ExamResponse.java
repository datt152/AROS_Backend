package vn.edu.aros.aroscore.dto.response;

import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;

@Data
public class ExamResponse {
    private Long id;
    private String title;
    private Integer duration;
    private ExamMode examMode;
    private ExamStatus status;
    private Long subjectId;
    private String subjectName;
    private String teacherEmail;
    private LocalDateTime createdAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer totalQuestions;
    private Double maxScore;
}
