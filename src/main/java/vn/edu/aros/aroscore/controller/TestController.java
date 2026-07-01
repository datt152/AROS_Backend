package vn.edu.aros.aroscore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.security.JwtUtils;

@RestController
public class TestController {
    @Autowired
    private JwtUtils jwtUtils;
    @GetMapping("/api/test")
    public String testConnection() {
        return "AROS Backend is running successfully!";
    }
    @GetMapping("/api/test-jwt")
    public String testJwt() {
        // 1. Giả lập một username để tạo token
        String mockUser = "dat_tran_2004";

        // 2. Tạo Token
        String token = jwtUtils.generateToken(mockUser);

        // 3. Kiểm tra xem Token có hợp lệ không
        boolean isValid = jwtUtils.validateJwtToken(token);

        // 4. Dịch ngược Token ra lại username
        String extractedUsername = jwtUtils.getUsernameFromToken(token);

        // Trả kết quả ra màn hình trình duyệt
        return "<h3>Kết quả test JWT:</h3>" +
                "<b>1. Chuỗi Token tạo ra:</b> " + token + "<br><br>" +
                "<b>2. Token có hợp lệ không?</b> " + isValid + "<br><br>" +
                "<b>3. Username giải mã được:</b> " + extractedUsername;
    }
}