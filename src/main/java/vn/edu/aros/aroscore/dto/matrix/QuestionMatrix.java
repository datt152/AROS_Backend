package vn.edu.aros.aroscore.dto.matrix;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMatrix {
    private Long originalQuestionId; // ID câu hỏi gốc trong Ngân hàng
    private Integer newOrder;        // Vị trí mới trong mã đề (Ví dụ: Câu số 1, số 2...)
    /** Nội dung câu hỏi đóng băng lúc sinh mã đề. */
    private String content;
    /** Loại câu hỏi đóng băng lúc sinh mã đề. */
    private QuestionType type;
    private List<AnswerMapping> answerMappings; // Danh sách các đáp án đã bị đảo
}
