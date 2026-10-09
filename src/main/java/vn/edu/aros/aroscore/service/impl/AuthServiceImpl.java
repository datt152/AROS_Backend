package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.config.AppMailProperties;
import vn.edu.aros.aroscore.dto.CustomUserDetails;
import vn.edu.aros.aroscore.dto.request.ForgotPasswordRequest;
import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.request.ResetPasswordRequest;
import vn.edu.aros.aroscore.dto.response.AuthSession;
import vn.edu.aros.aroscore.dto.response.LoginResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.PasswordResetToken;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.exception.UnauthorizedException;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.repository.PasswordResetTokenRepository;
import vn.edu.aros.aroscore.service.AuthService;
import vn.edu.aros.aroscore.service.CustomUserDetailsService;
import vn.edu.aros.aroscore.service.MailService;
import vn.edu.aros.aroscore.service.TurnstileService;
import vn.edu.aros.aroscore.utils.JwtUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String INVALID_RESET_TOKEN_MESSAGE =
            "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn!";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private CustomUserDetailsService customUserDetailsService;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private TurnstileService turnstileService;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private MailService mailService;
    @Autowired private AppMailProperties mailProperties;

    @Value("${app.password-reset.expiration-minutes:15}")
    private long resetExpirationMinutes;

    @Value("${app.password-reset.resend-cooldown-seconds:60}")
    private long resetResendCooldownSeconds;

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

    /** Luôn trả thành công cho caller, kể cả khi email không tồn tại, để không lộ tài khoản nào có trong hệ thống. */
    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        Optional<Account> accountOpt = accountRepository.findByEmailIgnoreCase(request.getEmail().trim());
        if (accountOpt.isEmpty()) {
            return;
        }
        Account account = accountOpt.get();
        LocalDateTime now = LocalDateTime.now();
        /** Kiểm tra cooldown để tránh spam. */
        boolean withinCooldown = passwordResetTokenRepository
                .findTopByAccountIdOrderByCreatedAtDesc(account.getId())
                .map(t -> t.getCreatedAt().plusSeconds(resetResendCooldownSeconds).isAfter(now))
                .orElse(false);
        if (withinCooldown) {
            return;
        }
        /** Xóa tất cả các token cũ để tránh sử dụng token đã hết hạn. */
        passwordResetTokenRepository.deleteAllByAccountId(account.getId());

        /** Tạo token mới. */
        String rawToken = generateResetToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .account(account)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(now.plusMinutes(resetExpirationMinutes))
                .createdAt(now)
                .build());

        User user = account.getUser();
        String fullName = user != null ? user.getFullName() : null;
        mailService.sendPasswordReset(account.getEmail(), fullName, buildResetUrl(rawToken), resetExpirationMinutes);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        /** Kiểm tra token hợp lệ. */
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(sha256Hex(request.getToken().trim()))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_RESET_TOKEN_MESSAGE));
        /** Kiểm tra token có hết hạn không. */
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(token);
            throw new IllegalArgumentException(INVALID_RESET_TOKEN_MESSAGE);
        }

        Account account = token.getAccount();
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        passwordResetTokenRepository.deleteAllByAccountId(account.getId());
    }

    private String buildResetUrl(String rawToken) {
        String base = mailProperties.getFrontendUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/reset-password?token=" + rawToken;
    }

    private static String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    private LoginResponse buildLoginResponse(CustomUserDetails userDetails, String accessToken) {
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        if (role.startsWith(ROLE_PREFIX)) {
            role = role.substring(ROLE_PREFIX.length());
        }
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .email(userDetails.getUsername())
                .role(UserRole.valueOf(role))
                .build();
    }
}
