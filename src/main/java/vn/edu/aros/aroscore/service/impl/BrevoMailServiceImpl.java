package vn.edu.aros.aroscore.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.aros.aroscore.config.AppMailProperties;
import vn.edu.aros.aroscore.service.MailService;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoMailServiceImpl implements MailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient brevoClient;
    private final TemplateEngine templateEngine;
    private final AppMailProperties mailProperties;

    public BrevoMailServiceImpl(AppMailProperties mailProperties, TemplateEngine templateEngine) {
        this.mailProperties = mailProperties;
        this.templateEngine = templateEngine;
        this.brevoClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", mailProperties.getBrevoApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    @Async
    public void sendStudentAccountCreated(String toEmail, String fullName, String temporaryPassword) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Bỏ qua gửi mail: email trống");
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName != null ? fullName : toEmail);
            context.setVariable("email", toEmail);
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("loginUrl", mailProperties.getFrontendUrl());
            context.setVariable("appName", mailProperties.getFromName());

            String html = templateEngine.process("mail/student-account-created", context);
            String subject = "[" + mailProperties.getFromName() + "] Tài khoản sinh viên đã được tạo";

            sendViaBrevo(toEmail, subject, html);
            log.info("Đã gửi mail tạo tài khoản tới {} qua Brevo", toEmail);
        } catch (Exception e) {
            log.error("Gửi mail tới {} qua Brevo thất bại: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordReset(String toEmail, String fullName, String resetUrl, long expirationMinutes) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Bỏ qua gửi mail đặt lại mật khẩu: email trống");
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName != null ? fullName : toEmail);
            context.setVariable("email", toEmail);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expirationMinutes", expirationMinutes);
            context.setVariable("appName", mailProperties.getFromName());

            String html = templateEngine.process("mail/password-reset", context);
            String subject = "[" + mailProperties.getFromName() + "] Đặt lại mật khẩu";

            sendViaBrevo(toEmail, subject, html);
            log.info("Đã gửi mail đặt lại mật khẩu tới {} qua Brevo", toEmail);
        } catch (Exception e) {
            log.error("Gửi mail đặt lại mật khẩu tới {} qua Brevo thất bại: {}", toEmail, e.getMessage());
        }
    }

    private void sendViaBrevo(String to, String subject, String htmlBody) {
        Map<String, Object> sender = Map.of(
                "name", mailProperties.getFromName(),
                "email", mailProperties.getFrom()
        );

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody
        );

        brevoClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
