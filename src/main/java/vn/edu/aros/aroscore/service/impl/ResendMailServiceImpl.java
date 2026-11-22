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
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailServiceImpl implements MailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestClient resendClient;
    private final TemplateEngine templateEngine;
    private final AppMailProperties mailProperties;

    public ResendMailServiceImpl(AppMailProperties mailProperties, TemplateEngine templateEngine) {
        this.mailProperties = mailProperties;
        this.templateEngine = templateEngine;
        this.resendClient = RestClient.builder()
                .baseUrl(RESEND_API_URL)
                .defaultHeader("Authorization", "Bearer " + mailProperties.getResendApiKey())
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

            sendViaResend(toEmail, subject, html);
            log.info("Đã gửi mail tạo tài khoản tới {} qua Resend", toEmail);
        } catch (Exception e) {
            log.error("Gửi mail tới {} qua Resend thất bại: {}", toEmail, e.getMessage());
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

            sendViaResend(toEmail, subject, html);
            log.info("Đã gửi mail đặt lại mật khẩu tới {} qua Resend", toEmail);
        } catch (Exception e) {
            log.error("Gửi mail đặt lại mật khẩu tới {} qua Resend thất bại: {}", toEmail, e.getMessage());
        }
    }

    private void sendViaResend(String to, String subject, String htmlBody) {
        String fromAddress = mailProperties.getFromName() + " <" + mailProperties.getFrom() + ">";

        Map<String, Object> body = Map.of(
                "from", fromAddress,
                "to", List.of(to),
                "subject", subject,
                "html", htmlBody
        );

        resendClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
