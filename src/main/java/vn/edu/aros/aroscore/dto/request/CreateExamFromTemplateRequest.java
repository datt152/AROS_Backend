package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateExamFromTemplateRequest {

    /** Override tiêu đề; null = giữ title của template. */
    private String title;

    @NotEmpty(message = "Phải giao ít nhất 1 lớp khi tạo đề từ template")
    private List<Long> classroomIds;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** Mặc định DRAFT nếu không gửi. */
    private ExamStatus status;
}
