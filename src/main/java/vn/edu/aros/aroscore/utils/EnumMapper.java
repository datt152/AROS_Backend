package vn.edu.aros.aroscore.utils;

import vn.edu.aros.aroscore.entity.enums.Difficulty;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

public class EnumMapper {

    public static QuestionType parseQuestionType(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại câu hỏi không được rỗng");
        }

        String trimmed = input.trim();
        
        // Tiếng Việt
        if (trimmed.equals("Một đáp án")) return QuestionType.SINGLE_CHOICE;
        if (trimmed.equals("Nhiều đáp án")) return QuestionType.MULTIPLE_CHOICE;
        
        // Tiếng Anh
        if (trimmed.equalsIgnoreCase("SINGLE") || 
            trimmed.equalsIgnoreCase("SINGLE_CHOICE")) {
            return QuestionType.SINGLE_CHOICE;
        }
        if (trimmed.equalsIgnoreCase("MULTIPLE") || 
            trimmed.equalsIgnoreCase("MULTIPLE_CHOICE")) {
            return QuestionType.MULTIPLE_CHOICE;
        }

        throw new IllegalArgumentException(
            "Loại câu hỏi không hợp lệ: '" + input + "'. " +
            "Chấp nhận: SINGLE, MULTIPLE, 'Một đáp án', 'Nhiều đáp án'"
        );
    }

    public static Difficulty parseDifficulty(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Difficulty.EASY; // Mặc định
        }

        String trimmed = input.trim();
        
        // Tiếng Việt
        if (trimmed.equals("Dễ")) return Difficulty.EASY;
        if (trimmed.equals("Trung bình")) return Difficulty.MEDIUM;
        if (trimmed.equals("Khó")) return Difficulty.HARD;
        if (trimmed.equals("Rất khó")) return Difficulty.VERY_HARD;
        if (trimmed.equals("Vận dụng")) return Difficulty.APPLICATION;
        
        // Tiếng Anh
        if (trimmed.equalsIgnoreCase("EASY")) return Difficulty.EASY;
        if (trimmed.equalsIgnoreCase("MEDIUM")) return Difficulty.MEDIUM;
        if (trimmed.equalsIgnoreCase("HARD")) return Difficulty.HARD;
        if (trimmed.equalsIgnoreCase("VERY_HARD")) return Difficulty.VERY_HARD;
        if (trimmed.equalsIgnoreCase("APPLICATION")) return Difficulty.APPLICATION;

        throw new IllegalArgumentException(
            "Độ khó không hợp lệ: '" + input + "'. " +
            "Chấp nhận: EASY, MEDIUM, HARD, VERY_HARD, APPLICATION " +
            "hoặc tiếng Việt: Dễ, Trung bình, Khó, Rất khó, Vận dụng"
        );
    }
}
