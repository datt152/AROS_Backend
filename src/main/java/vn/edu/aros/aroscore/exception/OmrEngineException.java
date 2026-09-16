package vn.edu.aros.aroscore.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class OmrEngineException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;
    private final String hint;
    private final Map<String, Object> details;

    public OmrEngineException(
            int httpStatus,
            String errorCode,
            String message,
            String hint,
            Map<String, Object> details) {
        super(message != null && !message.isBlank() ? message : "OMR scan thất bại");
        this.httpStatus = httpStatus;
        this.errorCode = errorCode != null && !errorCode.isBlank() ? errorCode : "OMR_UNKNOWN";
        this.hint = hint;
        this.details = details;
    }
}
