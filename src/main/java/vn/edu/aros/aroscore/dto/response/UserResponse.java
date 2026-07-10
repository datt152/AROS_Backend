package vn.edu.aros.aroscore.dto.response;


import lombok.Builder;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.UserRole;

@Data
@Builder
public class UserResponse {
    private Long id;
    private Long accountId;
    private String fullName;
    private String email;
    private String phone;
    private String studentCode;
    private UserRole role;
}