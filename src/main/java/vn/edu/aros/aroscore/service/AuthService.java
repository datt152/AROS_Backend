package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.response.LoginResponse;

public interface AuthService {
    public void registerAccount(RegisterRequest request);
    public LoginResponse authenticateUser(LoginRequest request);
}
