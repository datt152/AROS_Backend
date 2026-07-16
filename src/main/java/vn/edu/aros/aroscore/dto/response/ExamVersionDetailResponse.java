package vn.edu.aros.aroscore.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ExamVersionDetailResponse {
    private Long examId;
    private String title;
    private Integer duration;
    private String versionCode;
    private List<QuestionInVersionResponse> questions;
}