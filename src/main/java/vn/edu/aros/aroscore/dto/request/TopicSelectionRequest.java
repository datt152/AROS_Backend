package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicSelectionRequest {

    @NotNull(message = "Mã chủ đề không được để trống")
    private Long topicId;

    @NotNull(message = "Số câu không được để trống")
    @Min(value = 1, message = "Số câu phải >= 1")
    private Integer count;
}
