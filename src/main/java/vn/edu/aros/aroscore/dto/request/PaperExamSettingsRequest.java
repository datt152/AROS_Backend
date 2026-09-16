package vn.edu.aros.aroscore.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PaperExamSettingsRequest {
    private LocalDate examDate;
    private String semester;
    private String academicYear;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Integer paperCount;
}
