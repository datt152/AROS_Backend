package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ExamVersionCreateRequest {

    @NotNull(message = "ID của Đề gốc không được để trống")
    private Long examId;

    // Trường hợp 1: Giáo viên tự gõ mã (vd: ["101", "102", "103"])
    private List<String> manualVersionCodes;

    // Trường hợp 2: Mã đề tự sinh theo số lượng
    // Hệ thống sẽ tự sinh: "001", "002", "003", "004"
    private Integer autoGenerateCount;
}