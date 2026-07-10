package vn.edu.aros.aroscore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EnrollStudentRequest {

    @NotEmpty(message = "Danh sách email không được để trống")
    private List<String> studentEmails;
}
