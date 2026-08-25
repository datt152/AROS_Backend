package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.ExamType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamConfigResponse {
    private Long id;
    private String semester;
    private String academicYear;
    private Integer totalQuestions;
    private ExamType examType;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Integer paperCount;
    private Boolean allowEdit;
}
