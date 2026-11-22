package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TemplateGenerationRequest {
    @NotBlank(message = "Tiêu đề không được rỗng")
    private String title;

    @NotNull(message = "ID môn học không được rỗng")
    private Long subjectId;

    private Long topicId;  // Optional: Nếu null thì lấy tất cả topic của subject

    @NotNull(message = "Số lượng câu hỏi không được rỗng")
    @Min(value = 1, message = "Phải có ít nhất 1 câu hỏi")
    @Max(value = 200, message = "Tối đa 200 câu hỏi")
    private Integer totalQuestions;

    @NotNull(message = "Phải chỉ định loại đề (OMR hay Online)")
    private Boolean isOmr;

    @Min(value = 0, message = "Điểm không được âm")
    private Double pointsPerQuestion;  // Mặc định 1.0 nếu null
}
