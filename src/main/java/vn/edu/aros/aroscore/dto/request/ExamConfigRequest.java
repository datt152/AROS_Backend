package vn.edu.aros.aroscore.dto.request;

import lombok.Data;

@Data
public class ExamConfigRequest {
    private String semester;
    private String academicYear;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Integer paperCount;
    private Boolean allowEdit;
    private Boolean showScoreToStudent;
    private Integer maxAttempts;
    private Boolean timeLimitEnabled;
}
