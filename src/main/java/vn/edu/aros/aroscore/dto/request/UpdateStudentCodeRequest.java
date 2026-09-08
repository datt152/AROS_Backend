package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStudentCodeRequest {

    @NotBlank(message = "Mã sinh viên không được để trống")
    private String studentCode;
}
