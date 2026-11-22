package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamTemplateResponse {
    private Long id;
    private String title;
    private Long subjectId;
    private String subjectName;
    private String teacherEmail;
    private Integer totalQuestions;
    private List<Long> questionIds;
    /** questionId -> rawPoint */
    private Map<Long, Double> rawPoints;
    private LocalDateTime createdAt;
    private Boolean isActive;
    /** true = đề mẫu OMR, false = Online */
    private Boolean isOmr;
    /** Số đáp án tối đa (thường 4 OMR / 6 Online) */
    private Integer maxOptions;
}
