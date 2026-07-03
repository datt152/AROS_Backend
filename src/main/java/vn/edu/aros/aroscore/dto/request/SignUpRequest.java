package vn.edu.aros.aroscore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.UserRole;

@Data
public class SignUpRequest {
    private String password;
    @Email
    private String email;
    @Schema(description = "Vai trò người dùng", example = "STUDENT", allowableValues = {"STUDENT", "LECTURER", "ADMIN"})
    private UserRole role;
}