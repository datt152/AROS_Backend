package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.CustomUserDetails;
import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.response.AuthSession;
import vn.edu.aros.aroscore.dto.response.LoginResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.exception.UnauthorizedException;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.service.AuthService;
import vn.edu.aros.aroscore.service.CustomUserDetailsService;
import vn.edu.aros.aroscore.service.TurnstileService;
import vn.edu.aros.aroscore.utils.JwtUtils;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private CustomUserDetailsService customUserDetailsService;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private TurnstileService turnstileService;

    @Override
    @Transactional
    public void registerAccount(RegisterRequest request, String remoteIp) {
        turnstileService.verifyOrThrow(request.getCaptchaToken(), remoteIp);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        // Public self-register chỉ dành cho giáo viên
        if (request.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException("Chỉ được đăng ký tài khoản giáo viên!");
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống!");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setActive(true);

        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(UserRole.TEACHER);
        account.setUser(user);

        accountRepository.save(account);
        // captchaToken không được lưu DB (chỉ dùng để verify)
    }

    @Override
    public AuthSession authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        return AuthSession.builder()
                .loginResponse(buildLoginResponse(userDetails, jwtUtils.generateAccessToken(email)))
                .refreshToken(jwtUtils.generateRefreshToken(email))
                .build();
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtUtils.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtUtils.getEmailFromToken(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);
        return buildLoginResponse(userDetails, jwtUtils.generateAccessToken(email));
    }

    private LoginResponse buildLoginResponse(CustomUserDetails userDetails, String accessToken) {
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .email(userDetails.getUsername())
                .role(UserRole.valueOf(role))
                .build();
    }
}
