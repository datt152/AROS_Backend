package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

import java.util.List;

@Data
@Builder
public class QuestionTakeResponse {
    private Long questionId;
    private String content;
    private QuestionType type;
    private List<OptionTakeResponse> options;
}
