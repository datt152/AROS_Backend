package vn.edu.aros.aroscore.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.edu.aros.aroscore.config.AppMailProperties;
import vn.edu.aros.aroscore.service.MailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppMailProperties mailProperties;

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

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[" + mailProperties.getFromName() + "] Tài khoản sinh viên đã được tạo");
            if (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank()) {
                helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            }
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Đã gửi mail tạo tài khoản tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi mail tới {} thất bại: {}", toEmail, e.getMessage());
        }
    }
}
