package vn.edu.aros.aroscore.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vn.edu.aros.aroscore.config.TurnstileProperties;
import vn.edu.aros.aroscore.service.TurnstileService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnstileServiceImpl implements TurnstileService {

    private static final String FAIL_MESSAGE = "Xác minh captcha thất bại";

    private final TurnstileProperties turnstileProperties;

    @Override
    public void verifyOrThrow(String captchaToken, String remoteIp) {
        if (!StringUtils.hasText(captchaToken)) {
            throw new IllegalArgumentException("Vui lòng hoàn thành xác minh captcha");
        }
        if (!StringUtils.hasText(turnstileProperties.getSecretKey())) {
            log.error("TURNSTILE_SECRET_KEY is not configured");
            throw new IllegalArgumentException(FAIL_MESSAGE);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", turnstileProperties.getSecretKey());
        form.add("response", captchaToken.trim());
        if (StringUtils.hasText(remoteIp)) {
            form.add("remoteip", remoteIp.trim());
        }

        try {
            SiteverifyResponse response = restClient().post()
                    .uri(turnstileProperties.getSiteverifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteverifyResponse.class);

            if (response == null || !response.success()) {
                log.warn("Turnstile verification failed: errorCodes={}",
                        response != null ? response.errorCodes() : "null response");
                throw new IllegalArgumentException(FAIL_MESSAGE);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Turnstile siteverify request failed: {}", e.getMessage());
            throw new IllegalArgumentException(FAIL_MESSAGE);
        }
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(turnstileProperties.getConnectTimeoutMs());
        factory.setReadTimeout(turnstileProperties.getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SiteverifyResponse(
            boolean success,
            @JsonProperty("error-codes") java.util.List<String> errorCodes
    ) {}
}
