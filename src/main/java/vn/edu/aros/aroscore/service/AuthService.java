package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.SignUpRequest;

public interface AuthService {
    public void registerAccount(SignUpRequest request);
}
