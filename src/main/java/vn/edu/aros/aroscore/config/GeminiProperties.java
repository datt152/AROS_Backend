package vn.edu.aros.aroscore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    /** API key Gemini (không commit; đọc từ GEMINI_API_KEY). */
    private String apiKey = "";

    private String model = "gemini-3.8-flash";

    /** Endpoint generateContent (có thể đổi theo phiên bản API). */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 60000;

    /** Số lần thử lại khi Gemini trả 429/503 (high demand). */
    private int maxRetries = 3;

    /** Delay nền (ms) giữa các lần retry; lần n chờ nền * n. */
    private int retryDelayMs = 2000;
}
