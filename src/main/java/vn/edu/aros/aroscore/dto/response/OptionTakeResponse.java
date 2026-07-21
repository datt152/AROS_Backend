package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptionTakeResponse {
    private String label;
    private String content; // Nội dung đáp án
}