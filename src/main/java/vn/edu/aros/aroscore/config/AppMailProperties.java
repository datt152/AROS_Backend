package vn.edu.aros.aroscore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class AppMailProperties {

    /** smtp hoặc resend */
    private String provider = "smtp";

    /** Địa chỉ From (thường trùng SPRING_MAIL_USERNAME) */
    private String from = "";

    private String fromName = "AROS";

    /** URL frontend để gắn link đăng nhập trong email */
    private String frontendUrl = "http://localhost:5173";

    /** API key của Resend (chỉ cần khi provider=resend) */
    private String resendApiKey = "";
}
