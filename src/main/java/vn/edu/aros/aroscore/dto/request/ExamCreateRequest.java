package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import java.util.List;

@Data
public class ExamCreateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0")
    private Integer duration;

    @NotNull(message = "Hình thức thi không được để trống")
    private ExamMode examMode;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;

    @NotEmpty(message = "Đề thi phải có ít nhất 1 câu hỏi")
    private List<Long> questionIds; // Mảng chứa ID các câu hỏi được chọn
}