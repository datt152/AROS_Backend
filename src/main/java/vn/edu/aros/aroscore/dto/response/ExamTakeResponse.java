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
    private String versionCode; // Frontend cần mã đề này để nộp bài
    private List<QuestionTakeResponse> questions;
}