package vn.edu.aros.aroscore.config;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import vn.edu.aros.aroscore.filter.JwtAuthenticationFilter;
import vn.edu.aros.aroscore.service.CustomUserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Cho phép phân quyền bằng annotation @PreAuthorize trên Controller
public class SecurityConfig {

    @Autowired
    private CorsProperties corsProperties;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // 1. Cấu hình cỗ máy mã hóa mật khẩu (Chuẩn công nghiệp BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Cấu hình Provider cung cấp dữ liệu User cho Spring Security
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 3. Quản lý Authentication (Dùng để gọi lệnh xác thực lúc Login)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 4. BỘ CHỈ HUY: Phân quyền đường dẫn và nhét Filter vào
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Tắt CSRF vì chúng ta xài JWT, không xài Cookie
                .csrf(AbstractHttpConfigurer::disable)

                // Tắt Session hoàn toàn, bắt buộc mỗi request đều phải kẹp JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // FE dựa vào 401 để tự gọi /auth/refresh, nên chưa đăng nhập phải là 401 (không phải 403)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(request, response, HttpStatus.UNAUTHORIZED,
                                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(request, response, HttpStatus.FORBIDDEN,
                                        "Bạn không có quyền truy cập chức năng này")))

                // Phân luồng các endpoint
                .authorizeHttpRequests(auth ->
                        auth
                                // Cho phép vào tự do các API liên quan đến Auth
                                .requestMatchers("/api/v1/auth/**").permitAll()
                                .requestMatchers("/actuator/health").permitAll()

                                // Mở cửa hoàn toàn cho Swagger UI
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                                // Chấm phiếu OMR chỉ dành cho giáo viên
                                .requestMatchers("/api/v1/exam-sessions/**", "/api/v1/omr-sheets/**").hasRole("TEACHER")

                                // TẤT CẢ các đường dẫn còn lại đều bị khóa, bắt buộc phải có Token
                                .anyRequest().authenticated()
                );

        // Nạp Provider
        http.authenticationProvider(authenticationProvider());

        // Đặt cái "bác bảo vệ" JwtFilter đứng TRƯỚC cái Filter xác thực mặc định của Spring
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Áp dụng cho mọi API
        return source;
    }

    private static void writeError(HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {
        String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path));
    }
}