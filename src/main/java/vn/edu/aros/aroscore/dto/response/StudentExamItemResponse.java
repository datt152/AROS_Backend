package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamItemResponse {
    private Long examId;
    private String title;
    private ExamPurpose purpose;
    private ExamMode examMode;
    private ExamStatus examStatus;
    private GradingStatus myStatus;
    private Integer duration;
    private Boolean timeLimitEnabled;
    private Double maxScore;
    private Long subjectId;
    private String subjectName;
    private String teacherEmail;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer attemptNo;
    private Integer attemptsUsed;
    private Integer maxAttempts;
    private Long latestSubmissionId;
    private Double score;
    private Boolean scoreVisible;
    private Boolean canTake;
}
