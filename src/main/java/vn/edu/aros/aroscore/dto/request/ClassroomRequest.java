package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassroomRequest {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String className;

    private String description;

    private String semester;

    private String academicYear;

    private Boolean isActive = true;

    @NotNull(message = "Môn học ID không được để trống")
    private Long subjectId;
}