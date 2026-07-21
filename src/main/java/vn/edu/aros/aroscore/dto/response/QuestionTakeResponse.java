package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class QuestionTakeResponse {
    private Long questionId; // Cần ID gốc để học sinh nộp bài
    private String content;  // Nội dung câu hỏi
    private List<OptionTakeResponse> options;
}
