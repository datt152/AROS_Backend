package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicRequest {

    @NotBlank(message = "Tên chủ đề không được để trống")
    private String name;

    private String description;

    private Integer displayOrder;

    @NotNull(message = "Môn học ID không được để trống")
    private Long subjectId;
}
