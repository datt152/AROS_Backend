package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamTemplateResponse {
    private Long id;
    private String title;
    private Integer duration;
    private ExamMode examMode;
    private ExamPurpose purpose;
    private Long subjectId;
    private String subjectName;
    private String teacherEmail;
    private Double maxScore;
    private Integer totalQuestions;
    private List<Long> questionIds;
    private LocalDateTime createdAt;
    private Boolean isActive;

    private String semester;
    private String academicYear;
    private ExamType examType;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Integer paperCount;
    private Boolean allowEdit;
    private Boolean showScoreToStudent;
    private Integer maxAttempts;
    private Boolean timeLimitEnabled;
}
