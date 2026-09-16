package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.TemplateSelectionMode;

import java.util.List;
import java.util.Map;

@Data
public class ExamTemplateCreateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Môn học không được để trống")
    private Long subjectId;

    private TemplateSelectionMode selectionMode;

    /** Bắt buộc khi MANUAL. */
    private List<Long> questionIds;

    /** Bắt buộc khi BY_TOPIC. */
    @Valid
    private List<TopicSelectionRequest> topicSelections;

    private Map<Long, Double> rawPoints;
}
