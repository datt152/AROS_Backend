package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExamTemplateUpdateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;

    @NotEmpty(message = "Template phải có ít nhất 1 câu hỏi")
    private List<Long> questionIds;

    private Map<Long, Double> rawPoints;
}
