package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.response.AuthSession;
import vn.edu.aros.aroscore.dto.response.LoginResponse;

public interface AuthService {
    void registerAccount(RegisterRequest request, String remoteIp);
    AuthSession authenticateUser(LoginRequest request);
    LoginResponse refreshAccessToken(String refreshToken);
}
