package vn.edu.aros.aroscore.dto.request;

import lombok.Data;

/** Chỉ app mobile gửi body này; web để trống và dùng cookie httpOnly. */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
