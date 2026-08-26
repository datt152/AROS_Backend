package vn.edu.aros.aroscore.dto.response;

import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamResponse {
    private Long id;
    private String title;
    private Integer duration;
    private ExamMode examMode;
    private ExamPurpose purpose;
    private ExamStatus status;
    private Long subjectId;
    private String subjectName;
    private String teacherEmail;
    private LocalDateTime createdAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer totalQuestions;
    private Double maxScore;
    private List<Long> classroomIds;
    private ExamConfigResponse config;
    private Long sourceTemplateId;
}
