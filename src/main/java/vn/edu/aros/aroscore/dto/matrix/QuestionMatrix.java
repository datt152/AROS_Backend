package vn.edu.aros.aroscore.dto.matrix;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMatrix {
    private Long originalQuestionId; // ID câu hỏi gốc trong Ngân hàng
    private Integer newOrder;        // Vị trí mới trong mã đề (Ví dụ: Câu số 1, số 2...)
    private List<AnswerMapping> answerMappings; // Danh sách các đáp án đã bị đảo
}
