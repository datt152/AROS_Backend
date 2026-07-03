package vn.edu.aros.aroscore.config;


import org.springframework.beans.factory.annotation.Autowired;
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
import vn.edu.aros.aroscore.filter.JwtAuthenticationFilter;
import vn.edu.aros.aroscore.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Cho phép phân quyền bằng annotation @PreAuthorize trên Controller
public class SecurityConfig {

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
                // Tắt CSRF vì chúng ta xài JWT, không xài Cookie
                .csrf(AbstractHttpConfigurer::disable)

                // Tắt Session hoàn toàn, bắt buộc mỗi request đều phải kẹp JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân luồng các endpoint
                .authorizeHttpRequests(auth ->
                        auth
                                // Cho phép vào tự do các API liên quan đến Auth và Test
                                .requestMatchers("/api/v1/auth/**").permitAll()
                                .requestMatchers("/api/test-jwt").permitAll()

                                // Mở cửa hoàn toàn cho Swagger UI
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                                // TẤT CẢ các đường dẫn còn lại đều bị khóa, bắt buộc phải có Token
                                .anyRequest().authenticated()
                );

        // Nạp Provider
        http.authenticationProvider(authenticationProvider());

        // Đặt cái "bác bảo vệ" JwtFilter đứng TRƯỚC cái Filter xác thực mặc định của Spring
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}