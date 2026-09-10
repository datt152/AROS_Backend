package vn.edu.aros.aroscore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "turnstile")
public class TurnstileProperties {

    /** Cloudflare Turnstile Secret Key (server-only). */
    private String secretKey = "";

    private String siteverifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 10000;
}
