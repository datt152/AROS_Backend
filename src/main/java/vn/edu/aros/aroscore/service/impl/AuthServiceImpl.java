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
import vn.edu.aros.aroscore.utils.JwtUtils;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private CustomUserDetailsService customUserDetailsService;
    @Autowired private JwtUtils jwtUtils;

    @Override
    @Transactional
    public void registerAccount(RegisterRequest request) {
        // 1. Kiểm tra mật khẩu xác nhận
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        // 2. Chỉ cho phép đăng ký TEACHER hoặc STUDENT
        UserRole role = request.getRole();
        if (role != UserRole.TEACHER && role != UserRole.STUDENT) {
            throw new RuntimeException("Role đăng ký chỉ được phép là TEACHER hoặc STUDENT!");
        }

        // 3. Kiểm tra trùng lặp Email (1 email = 1 account / 1 role)
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // 4. Khởi tạo Profile User trước
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setActive(true);

        // 5. Khởi tạo Account và liên kết với User
        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(role);

        // Gắn User vào Account. Nhờ cascade = CascadeType.ALL ở Account, User sẽ tự động được lưu.
        account.setUser(user);

        // 6. Lưu xuống Database
        accountRepository.save(account);
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