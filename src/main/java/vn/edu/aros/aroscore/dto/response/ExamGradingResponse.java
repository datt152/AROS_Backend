package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamGradingResponse {
    private Long examId;
    private String examTitle;
    private Long classroomId;
    private String classroomName;
    private Double maxScore;
    private List<ExamGradingStudentResponse> students;
}
