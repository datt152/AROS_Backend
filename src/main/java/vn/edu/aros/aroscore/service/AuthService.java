package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.response.AuthSession;
import vn.edu.aros.aroscore.dto.response.LoginResponse;

public interface AuthService {
    public void registerAccount(RegisterRequest request);
    public AuthSession authenticateUser(LoginRequest request);
    public LoginResponse refreshAccessToken(String refreshToken);
}
