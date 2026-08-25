package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignExamClassroomsRequest {

    @NotEmpty(message = "Danh sách lớp không được để trống")
    private List<Long> classroomIds;
}
