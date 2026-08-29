package vn.edu.aros.aroscore.dto.response;

import lombok.Data;

@Data
public class SubjectResponse {
    private Long id;
    private String subjectName;
    private String description;
    private Long lecturerId;
    private Boolean isActive;
}
