package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class SubmissionRequest {

    @NotNull(message = "ID kỳ thi không được trống")
    private Long examId;

    @NotBlank(message = "Mã đề không được trống")
    private String versionCode;

    // Key: ID câu hỏi gốc, Value: Đáp án học sinh chọn (ví dụ: "A" hoặc "A,B")
    @NotNull(message = "Danh sách đáp án không được trống")
    private Map<Long, String> answers;
}