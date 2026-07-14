package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.Difficulty;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

import java.util.List;

@Data
public class QuestionRequest {
    @NotNull(message = "ID Môn học không được rỗng")
    private Long subjectId;

    @NotBlank(message = "Nội dung câu hỏi không được rỗng")
    private String content;

    private Difficulty difficulty;

    private String explanation;

    @NotEmpty(message = "Phải có ít nhất 1 đáp án")
    private List<AnswerOptionRequest> options;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private QuestionType type;
}