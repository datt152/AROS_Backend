package vn.edu.aros.aroscore.dto.response;

import lombok.Data;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import java.util.List;

@Data
public class QuestionInVersionResponse {
    private Long originalQuestionId;
    private String content;
    private QuestionType type;
    private List<OptionInVersionResponse> options;
}