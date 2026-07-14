package vn.edu.aros.aroscore.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.dto.request.AnswerOptionRequest;
import vn.edu.aros.aroscore.entity.enums.Difficulty;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long subjectId;

    private String content;

    private Difficulty difficulty;

    private String explanation;

    private List<AnswerOptionRequest> options;

    private QuestionType type;
}
