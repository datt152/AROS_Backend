package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tạo Exam từ template: lấy câu hỏi từ thư viện,
 * còn cấu hình đợt thi do request này quyết định.
 */
@Data
public class CreateExamFromTemplateRequest {

    /** Override tiêu đề; null = dùng title của template. */
    private String title;

    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0")
    private Integer duration;

    @NotNull(message = "Hình thức thi không được để trống")
    private ExamMode examMode;

    /** Mặc định EXAM. */
    private ExamPurpose purpose;

    @NotNull(message = "Thang điểm chuẩn không được để trống")
    private Double maxScore;

    private ExamConfigRequest config;

    /** Optional — có thể giao lớp sau. */
    private List<Long> classroomIds;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** Chỉ cho phép DRAFT (mặc định). */
    private ExamStatus status;
}
