package vn.edu.aros.aroscore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String email;
    private UserRole role;

    /** Chỉ có giá trị với app mobile (header X-Client-Type: mobile); web nhận qua cookie httpOnly. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;
}