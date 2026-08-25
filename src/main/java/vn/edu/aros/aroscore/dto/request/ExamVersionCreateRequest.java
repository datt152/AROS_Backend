package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExamVersionCreateRequest {

    @NotNull(message = "ID của Đề gốc không được để trống")
    private Long examId;

    private List<String> manualVersionCodes;

    private Integer autoGenerateCount;

    /** Nếu true: ghi đè các versionCode đã tồn tại. Mặc định false → báo lỗi trùng. */
    private Boolean replaceExisting = false;
}
