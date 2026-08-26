package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponse {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private Long subjectId;
    private String subjectName;
    private Boolean isActive;
    private Long questionCount;
}
