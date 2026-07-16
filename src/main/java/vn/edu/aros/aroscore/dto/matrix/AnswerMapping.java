package vn.edu.aros.aroscore.dto.matrix;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerMapping {
    private Long originalOptionId;
    private String newLabel;       // Nhãn mới sau khi trộn (Ví dụ: "A", "B", "C", "D")
    private Boolean isCorrect;
}