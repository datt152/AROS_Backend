package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    /**
     * Tuỳ chọn. Chỉ áp dụng cho STUDENT; nếu gửi lên sẽ validate 8 số + không trùng trong lớp.
     * Để trống / null = không đổi mã sinh viên.
     */
    private String studentCode;
}
