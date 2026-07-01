package vn.edu.aros.aroscore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.UserRole;

@Data
public class SignUpRequest {
    private String username;
    private String password;
    private String email;
    @Schema(description = "Vai trò người dùng", example = "STUDENT", allowableValues = {"STUDENT", "LECTURER", "ADMIN"})
    private UserRole role;
}