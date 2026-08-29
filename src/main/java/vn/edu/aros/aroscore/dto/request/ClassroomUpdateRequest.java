package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassroomUpdateRequest {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String className;

    private String description;

    private String semester;

    private String academicYear;

    private Boolean isActive;
}
