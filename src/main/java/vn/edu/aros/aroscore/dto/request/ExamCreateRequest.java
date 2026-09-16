package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
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

    /** Mặc định EXAM nếu không gửi. */
    private ExamPurpose purpose;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;

    @NotEmpty(message = "Đề thi phải có ít nhất 1 câu hỏi")
    private List<Long> questionIds;

    @NotNull(message = "Thang điểm chuẩn không được để trống (VD: 10.0)")
    private Double maxScore;

    private java.util.Map<Long, Double> rawPoints;

    private List<Long> classroomIds;

    /** Bắt buộc khi ONLINE. */
    private OnlineExamSettingsRequest onlineSettings;

    /** Bắt buộc khi OMR*/
    private PaperExamSettingsRequest paperSettings;
}
