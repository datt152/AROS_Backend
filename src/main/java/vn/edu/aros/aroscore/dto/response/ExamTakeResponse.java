package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ExamTakeResponse {
    private Long examId;
    private String title;
    private Integer duration;
    private String versionCode;
    private java.time.LocalDateTime startTime;
    private List<QuestionTakeResponse> questions;
}