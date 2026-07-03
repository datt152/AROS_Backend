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
import vn.edu.aros.aroscore.dto.response.LoginResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.service.AuthService;
import vn.edu.aros.aroscore.utils.JwtUtils;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtils jwtUtils;

    @Override
    @Transactional
    public void registerAccount(RegisterRequest request) {
        // 1. Kiểm tra mật khẩu xác nhận
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        // 2. Kiểm tra trùng lặp Email
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // 3. Khởi tạo Profile User trước
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setActive(true);

        // 4. Khởi tạo Account và liên kết với User
        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(request.getRole());

        // Gắn User vào Account. Nhờ cascade = CascadeType.ALL ở Account, User sẽ tự động được lưu.
        account.setUser(user);

        // 5. Lưu xuống Database
        accountRepository.save(account);
    }

    @Override
    public LoginResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String email = userDetails.getUsername();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        String jwt = jwtUtils.generateToken(email);

        return LoginResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .email(email)
                .role(UserRole.valueOf(role))
                .build();
    }
}