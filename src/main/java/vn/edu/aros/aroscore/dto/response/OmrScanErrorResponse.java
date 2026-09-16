package vn.edu.aros.aroscore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Lỗi OMR trả về Frontend — cùng contract với OMR Engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OmrScanErrorResponse {

    @Builder.Default
    private boolean success = false;

    @JsonProperty("error_code")
    private String errorCode;

    private String message;
    private String hint;
    private Map<String, Object> details;

    private Integer status;
    private String path;
}
