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
import org.springframework.web.util.UriComponentsBuilder;
import vn.edu.aros.aroscore.config.OmrProperties;
import vn.edu.aros.aroscore.dto.omr.OmrScanResponse;
import vn.edu.aros.aroscore.exception.OmrRetakeRequiredException;

import java.nio.file.Path;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class OmrEngineClient {

    private static final Pattern OMR_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final RestClient.Builder restClientBuilder;
    private final OmrProperties omrProperties;

    /**
     * Gọi OMR Engine POST /scan với teacher_id, session_id, submission_id (folder Cloudinary).
     */
    public OmrScanResult scan(Path imagePath, String teacherId, String sessionId, String submissionId) {
        String safeTeacherId = requireValidOmrId(teacherId, "teacher_id");
        String safeSessionId = requireValidOmrId(sessionId, "session_id");
        String safeSubmissionId = requireValidOmrId(submissionId, "submission_id");

        int maxAttempts = Math.max(1, omrProperties.getEngine().getMaxRetries() + 1);
        RestClientException lastError = null;

        String uri = UriComponentsBuilder.fromPath("/scan")
                .queryParam("teacher_id", safeTeacherId)
                .queryParam("session_id", safeSessionId)
                .queryParam("submission_id", safeSubmissionId)
                .build()
                .toUriString();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RestClient client = restClientBuilder.baseUrl(omrProperties.getEngine().getBaseUrl()).build();
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new FileSystemResource(imagePath));

                OmrScanResponse response = client.post()
                        .uri(uri)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .body(OmrScanResponse.class);

                return new OmrScanResult(true, response, null);
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == 422) {
                    throw new OmrRetakeRequiredException(extractRetakeMessage(e.getResponseBodyAsString()));
                }
                if (e.getStatusCode().value() == 400) {
                    throw new IllegalArgumentException("OMR từ chối tham số scan: " + e.getResponseBodyAsString());
                }
                lastError = e;
            } catch (RestClientException e) {
                lastError = e;
            }
        }

        return new OmrScanResult(false, null, lastError != null ? lastError.getMessage() : "OMR timeout");
    }

    public static String requireValidOmrId(String id, String fieldName) {
        if (id == null || id.isBlank() || !OMR_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "OMR " + fieldName + " không hợp lệ (chỉ chữ/số/_/-, tối đa 64 ký tự): " + id);
        }
        return id;
    }

    private static String extractRetakeMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Cần chụp lại ảnh phiếu";
        }
        return body;
    }

    public record OmrScanResult(boolean success, OmrScanResponse response, String errorMessage) {}
}
