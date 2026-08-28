package vn.edu.aros.aroscore.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import vn.edu.aros.aroscore.config.OmrProperties;
import vn.edu.aros.aroscore.dto.omr.OmrScanResponse;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class OmrEngineClient {

    private final RestClient.Builder restClientBuilder;
    private final OmrProperties omrProperties;

    public OmrScanResult scan(Path imagePath, Long submissionId) {
        int maxAttempts = Math.max(1, omrProperties.getEngine().getMaxRetries() + 1);
        RestClientException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RestClient client = restClientBuilder.baseUrl(omrProperties.getEngine().getBaseUrl()).build();
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new FileSystemResource(imagePath));

                String uri = submissionId != null
                        ? "/scan?submission_id=" + submissionId
                        : "/scan";

                OmrScanResponse response = client.post()
                        .uri(uri)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .body(OmrScanResponse.class);

                return new OmrScanResult(true, response, null);
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == 422) {
                    throw new OmrRetakeRequiredException(e.getResponseBodyAsString());
                }
                lastError = e;
            } catch (RestClientException e) {
                lastError = e;
            }
        }

        return new OmrScanResult(false, null, lastError != null ? lastError.getMessage() : "OMR timeout");
    }

    public record OmrScanResult(boolean success, OmrScanResponse response, String errorMessage) {}

    public static class OmrRetakeRequiredException extends RuntimeException {
        public OmrRetakeRequiredException(String message) {
            super(message);
        }
    }
}
