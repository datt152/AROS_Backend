package vn.edu.aros.aroscore.service;

/**
 * Gửi email hệ thống. Giai đoạn 3 sẽ gọi khi bulk tạo account.
 */
public interface MailService {

    /**
     * Gửi mail thông báo tài khoản sinh viên vừa được tạo (async).
     */
    void sendStudentAccountCreated(String toEmail, String fullName, String temporaryPassword);
}
