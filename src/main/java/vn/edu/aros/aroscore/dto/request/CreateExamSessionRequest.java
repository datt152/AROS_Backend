package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateExamSessionRequest {

    @NotNull(message = "examId không được để trống")
    private Long examId;

    @NotBlank(message = "Tên phiên chấm không được để trống")
    private String name;
}
