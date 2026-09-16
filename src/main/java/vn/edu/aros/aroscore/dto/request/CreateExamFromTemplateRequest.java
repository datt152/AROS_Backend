package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;

import java.util.List;

@Data
public class CreateExamFromTemplateRequest {

    private String title;

    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0")
    private Integer duration;

    @NotNull(message = "Hình thức thi không được để trống")
    private ExamMode examMode;

    private ExamPurpose purpose;

    @NotNull(message = "Thang điểm chuẩn không được để trống")
    private Double maxScore;

    private OnlineExamSettingsRequest onlineSettings;

    private PaperExamSettingsRequest paperSettings;

    private List<Long> classroomIds;
}
