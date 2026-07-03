package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.SignUpRequest;
import vn.edu.aros.aroscore.dto.response.LoginResponse;

public interface AuthService {
    public void registerAccount(SignUpRequest request);
    public LoginResponse authenticateUser(LoginRequest request);
}
