package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerOptionRequest {
    @NotBlank(message = "Nội dung đáp án không được rỗng")
    private String content;

    @NotNull(message = "Phải xác định đáp án đúng/sai")
    private Boolean isCorrect;
}