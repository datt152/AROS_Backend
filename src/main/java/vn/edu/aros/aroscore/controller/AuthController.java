package vn.edu.aros.aroscore.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.request.ForgotPasswordRequest;
import vn.edu.aros.aroscore.dto.request.LoginRequest;
import vn.edu.aros.aroscore.dto.request.RefreshTokenRequest;
import vn.edu.aros.aroscore.dto.request.RegisterRequest;
import vn.edu.aros.aroscore.dto.request.ResetPasswordRequest;
import vn.edu.aros.aroscore.dto.response.AuthSession;
import vn.edu.aros.aroscore.dto.response.LoginResponse;
import vn.edu.aros.aroscore.entity.enums.AuthClientType;
import vn.edu.aros.aroscore.service.AuthService;
import vn.edu.aros.aroscore.utils.JwtUtils;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";
    /** Phải bao cả /refresh và /logout để server đọc được token khi đăng xuất. */
    public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    /** Cookie cũ (trước khi đổi path) vẫn được trình duyệt gửi trước cookie mới nên phải xóa đi. */
    private static final String LEGACY_REFRESH_COOKIE_PATH = "/api/v1/auth/refresh";
    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${auth.refresh-cookie.secure:true}")
    private boolean refreshCookieSecure;

    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        authService.registerAccount(request, resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body("Đăng ký tài khoản thành công!");
    }

    /**
     * Web: refresh token trả qua cookie httpOnly.
     * Mobile (header {@code X-Client-Type: mobile}): refresh token trả trong body.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthClientType clientType = resolveClientType(httpRequest);
        AuthSession session = authService.authenticateUser(request, clientType);
        return buildSessionResponse(session, clientType);
    }

    /** Mỗi lần refresh sẽ cấp refresh token mới, token cũ bị vô hiệu. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest httpRequest) {
        AuthSession session = authService.refreshSession(extractRefreshToken(body, httpRequest));
        return buildSessionResponse(session, resolveClientType(httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest httpRequest) {
        authService.logout(extractRefreshToken(body, httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildRefreshCookie("", 0, REFRESH_COOKIE_PATH).toString(),
                        buildRefreshCookie("", 0, LEGACY_REFRESH_COOKIE_PATH).toString())
                .body("Đăng xuất thành công!");
    }

    /**
     * Bước 1 quên mật khẩu: gửi link đặt lại qua email. Luôn trả cùng một message dù email có tồn tại hay không.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok("Yêu cầu đặt lại mật khẩu đã được gửi qua email của bạn.");
    }

    /**
     * Bước 2 quên mật khẩu: đặt mật khẩu mới bằng token trong email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
    }

    private ResponseEntity<LoginResponse> buildSessionResponse(AuthSession session, AuthClientType clientType) {
        LoginResponse body = session.getLoginResponse();
        String refreshToken = session.getRefreshToken();

        if (clientType == AuthClientType.MOBILE) {
            body.setRefreshToken(refreshToken);
            return ResponseEntity.ok(body);
        }
        // null = refresh song song trong khoảng ân hạn, trình duyệt đã có cookie mới từ request kia
        if (refreshToken == null) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        buildRefreshCookie(refreshToken, jwtUtils.getRefreshExpirationSeconds(), REFRESH_COOKIE_PATH)
                                .toString(),
                        buildRefreshCookie("", 0, LEGACY_REFRESH_COOKIE_PATH).toString())
                .body(body);
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, long maxAgeSeconds, String path) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private AuthClientType resolveClientType(HttpServletRequest request) {
        return "mobile".equalsIgnoreCase(request.getHeader(CLIENT_TYPE_HEADER))
                ? AuthClientType.MOBILE
                : AuthClientType.WEB;
    }

    /** Ưu tiên body (mobile), sau đó tới cookie (web). */
    private String extractRefreshToken(RefreshTokenRequest body, HttpServletRequest request) {
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            return body.getRefreshToken();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
