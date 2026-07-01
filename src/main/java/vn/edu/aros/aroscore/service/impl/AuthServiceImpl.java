package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.aros.aroscore.dto.CustomUserDetails;
import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.SignUpRequest;
import vn.edu.aros.aroscore.dto.response.LoginResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.service.AuthService;
import vn.edu.aros.aroscore.utils.JwtUtils;

@Service
public class AuthServiceImpl  implements AuthService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void registerAccount(SignUpRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        // Tạo Entity
        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword())); // MÃ HÓA PASSWORD
        account.setRole(request.getRole());

        accountRepository.save(account);
    }

    @Override
    public LoginResponse authenticateUser(LoginRequest request) {
        // 1. Xác thực email/password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // 2. Lấy thông tin user đã xác thực
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 3. Lấy email (lúc này chính là username trong UserDetails) và role
        String email = userDetails.getUsername();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        // 4. Tạo token bằng email
        String jwt = jwtUtils.generateToken(email);

        return LoginResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .email(email)
                .role(UserRole.valueOf(role))
                .build();
    }
}
