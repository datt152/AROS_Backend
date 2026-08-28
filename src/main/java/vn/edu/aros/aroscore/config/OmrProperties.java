package vn.edu.aros.aroscore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "omr")
public class OmrProperties {

    private Engine engine = new Engine();
    private Upload upload = new Upload();

    @Getter
    @Setter
    public static class Engine {
        private String baseUrl = "http://localhost:8000";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 60000;
        private int maxRetries = 1;
    }

    @Getter
    @Setter
    public static class Upload {
        private String dir = "uploads/omr";
        private long maxSizeMb = 10;
    }
}
