package vn.edu.aros.aroscore.dto.omr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Body lỗi chuẩn từ OMR Engine POST /scan.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OmrEngineErrorBody {

    private Boolean success;

    @JsonProperty("error_code")
    private String errorCode;

    /** Legacy — engine cũ dùng error: "retake_required". */
    private String error;

    private String message;
    private String hint;
    private Map<String, Object> details;

    /** FastAPI validation fallback. */
    private Object detail;
}
