package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubjectRequest {
    @NotBlank(message = "Tên môn học không được để trống")
    private String subjectName;

    private String description;
}