package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.ForgotPasswordRequest;
import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.request.ResetPasswordRequest;
import vn.edu.aros.aroscore.dto.response.AuthSession;
import vn.edu.aros.aroscore.entity.enums.AuthClientType;

public interface AuthService {
    void registerAccount(RegisterRequest request, String remoteIp);
    AuthSession authenticateUser(LoginRequest request, AuthClientType clientType);
    AuthSession refreshSession(String refreshToken);
    void logout(String refreshToken);
    void requestPasswordReset(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
