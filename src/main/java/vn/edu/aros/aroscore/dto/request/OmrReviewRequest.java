package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class OmrReviewRequest {

    /** questionNumber -> chosen answer (A/B/C/D) */
    @NotEmpty(message = "Danh sách đáp án sửa không được rỗng")
    private Map<Integer, String> answers;
}
