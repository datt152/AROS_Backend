package vn.edu.aros.aroscore.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import vn.edu.aros.aroscore.dto.omr.OmrEngineErrorBody;
import vn.edu.aros.aroscore.dto.omr.OmrScanResponse;
import vn.edu.aros.aroscore.exception.OmrEngineException;

import java.nio.file.Path;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class OmrEngineClient {

    private static final Pattern OMR_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final RestClient.Builder restClientBuilder;
    private final OmrProperties omrProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gọi OMR Engine POST /scan với teacher_id, session_id, submission_id.
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

                if (response != null && Boolean.FALSE.equals(response.getSuccess())
                        && (response.getErrorCode() != null || response.getError() != null)) {
                    throw toOmrEngineException(422, toErrorBody(response));
                }

                return new OmrScanResult(true, response, null);
            } catch (OmrEngineException e) {
                throw e;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 400 || status == 422) {
                    throw parseEngineError(status, e.getResponseBodyAsString());
                }
                log.warn("OMR scan HTTP {} (attempt {}/{}): {}",
                        status, attempt, maxAttempts, e.getResponseBodyAsString());
                lastError = e;
            } catch (RestClientException e) {
                log.warn("OMR scan I/O (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
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

    private OmrEngineException parseEngineError(int httpStatus, String body) {
        OmrEngineErrorBody parsed = null;
        if (body != null && !body.isBlank()) {
            try {
                parsed = objectMapper.readValue(body, OmrEngineErrorBody.class);
            } catch (Exception e) {
                log.debug("Không parse được OMR error body: {}", e.getMessage());
            }
        }
        if (parsed == null) {
            parsed = new OmrEngineErrorBody();
            parsed.setMessage(body != null && !body.isBlank() ? body : "OMR scan thất bại");
        }
        return toOmrEngineException(httpStatus, parsed);
    }

    private OmrEngineException toOmrEngineException(int httpStatus, OmrEngineErrorBody body) {
        String errorCode = body.getErrorCode();
        if (errorCode == null || errorCode.isBlank()) {
            if ("retake_required".equalsIgnoreCase(body.getError())) {
                errorCode = "OMR_RET_REQUIRED";
            } else if (body.getDetail() != null) {
                errorCode = "OMR_VALIDATION_ERROR";
            } else {
                errorCode = httpStatus == 422 ? "OMR_RET_REQUIRED" : "OMR_BAD_REQUEST";
            }
        }

        String message = body.getMessage();
        if ((message == null || message.isBlank()) && body.getDetail() != null) {
            message = String.valueOf(body.getDetail());
        }
        if (message == null || message.isBlank()) {
            message = "OMR scan thất bại";
        }

        return new OmrEngineException(httpStatus, errorCode, message, body.getHint(), body.getDetails());
    }

    private OmrEngineErrorBody toErrorBody(OmrScanResponse response) {
        OmrEngineErrorBody body = new OmrEngineErrorBody();
        body.setSuccess(false);
        body.setErrorCode(response.getErrorCode());
        body.setError(response.getError());
        body.setMessage(response.getMessage());
        body.setHint(response.getHint());
        return body;
    }

    public record OmrScanResult(boolean success, OmrScanResponse response, String errorMessage) {}
}
