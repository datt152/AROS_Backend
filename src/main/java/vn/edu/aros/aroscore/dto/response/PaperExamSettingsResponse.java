package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperExamSettingsResponse {
    private Long id;
    private LocalDate examDate;
    private String semester;
    private String academicYear;
    private Integer totalQuestions;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Integer paperCount;
}
