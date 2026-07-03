package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomResponse {
    private Long id;
    private String className;
    private String description;
    private String semester;
    private String academicYear;
    private Boolean isActive;
    private Long subjectId;
    private String subjectName;
}