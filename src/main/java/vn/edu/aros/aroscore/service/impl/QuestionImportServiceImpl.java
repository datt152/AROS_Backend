package vn.edu.aros.aroscore.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.response.QuestionImportResult;
import vn.edu.aros.aroscore.entity.AnswerOption;
import vn.edu.aros.aroscore.entity.Question;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.Topic;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.Difficulty;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.repository.QuestionRepository;
import vn.edu.aros.aroscore.repository.TopicRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.QuestionImportService;
import vn.edu.aros.aroscore.utils.EnumMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class QuestionImportServiceImpl implements QuestionImportService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public QuestionImportResult importQuestionsFromExcel(
        MultipartFile file,
        Long topicId,
        Long teacherId
    ) {
        List<QuestionImportResult.RowError> errors = new ArrayList<>();
        List<Question> validQuestions = new ArrayList<>();
        int totalRows = 0;

        // Validate topic và teacher
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy Topic với ID: " + topicId));
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy Teacher với ID: " + teacherId));
        
        Subject subject = topic.getSubject();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            for (Row row : sheet) {
                // Skip header row
                if (row.getRowNum() == 0) continue;
                
                totalRows++;
                int rowNumber = row.getRowNum() + 1;

                try {
                    // Parse dòng Excel
                    Question question = parseRowToQuestion(row, topic, subject, teacher);
                    
                    // Validate câu hỏi
                    validateQuestion(question);
                    
                    // Thêm vào danh sách hợp lệ
                    validQuestions.add(question);
                    
                } catch (Exception e) {
                    String content = getCellValue(row, 1); // Column B: Nội dung câu hỏi
                    errors.add(QuestionImportResult.RowError.builder()
                        .rowNumber(rowNumber)
                        .questionContent(content != null ? content : "(Trống)")
                        .errorMessages(List.of(e.getMessage()))
                        .build());
                }
            }

            // Lưu tất cả câu hỏi hợp lệ
            if (!validQuestions.isEmpty()) {
                questionRepository.saveAll(validQuestions);
            }

        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file Excel: " + e.getMessage(), e);
        }

        return QuestionImportResult.builder()
            .totalRows(totalRows)
            .successCount(validQuestions.size())
            .errorCount(errors.size())
            .errors(errors)
            .build();
    }

    /**
     * Parse 1 dòng Excel thành Question entity
     */
    private Question parseRowToQuestion(Row row, Topic topic, Subject subject, User teacher) {
        // Đọc các cột theo thứ tự:
        // A: STT, B: Nội dung, C: Loại, D: Độ khó, E: Giải thích
        // F-K: Đáp án A-F, L: Đáp án đúng
        
        String content = getCellValue(row, 1); // Column B
        String typeStr = getCellValue(row, 2); // Column C
        String difficultyStr = getCellValue(row, 3); // Column D
        String explanation = getCellValue(row, 4); // Column E
        
        // Parse 6 đáp án (A-F)
        String[] answerContents = new String[6];
        for (int i = 0; i < 6; i++) {
            answerContents[i] = getCellValue(row, 5 + i); // Columns F-K
        }
        
        String correctAnswersStr = getCellValue(row, 11); // Column L

        // Validate nội dung cơ bản
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được rỗng");
        }
        
        if (typeStr == null || typeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại câu hỏi không được rỗng");
        }
        
        if (correctAnswersStr == null || correctAnswersStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Đáp án đúng không được rỗng");
        }

        // Parse enum
        QuestionType type = EnumMapper.parseQuestionType(typeStr);
        Difficulty difficulty = EnumMapper.parseDifficulty(difficultyStr);

        // Parse đáp án đúng
        List<String> correctLabels = parseCorrectAnswers(correctAnswersStr, type);

        // Đếm số đáp án không trống
        List<String> nonEmptyAnswers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (answerContents[i] != null && !answerContents[i].trim().isEmpty()) {
                nonEmptyAnswers.add(answerContents[i]);
            }
        }

        if (nonEmptyAnswers.size() < 2) {
            throw new IllegalArgumentException("Phải có ít nhất 2 đáp án");
        }

        // Build Question
        Question question = Question.builder()
            .content(content.trim())
            .type(type)
            .difficulty(difficulty)
            .explanation(explanation != null ? explanation.trim() : null)
            .subject(subject)
            .topic(topic)
            .teacher(teacher)
            .isActive(true)
            .build();

        // Build Answer Options
        char[] labels = {'A', 'B', 'C', 'D', 'E', 'F'};
        for (int i = 0; i < nonEmptyAnswers.size(); i++) {
            String label = String.valueOf(labels[i]);
            boolean isCorrect = correctLabels.contains(label);
            
            AnswerOption option = AnswerOption.builder()
                .content(nonEmptyAnswers.get(i).trim())
                .isCorrect(isCorrect)
                .build();
            
            question.addOption(option);
        }

        return question;
    }

    /**
     * Parse chuỗi đáp án đúng (A, B,C, A,B,C,D...)
     */
    private List<String> parseCorrectAnswers(String input, QuestionType type) {
        String normalized = input.toUpperCase().replaceAll("\\s+", "");
        
        List<String> labels;
        if (normalized.contains(",")) {
            labels = Arrays.asList(normalized.split(","));
        } else {
            labels = List.of(normalized);
        }

        // Validate format
        for (String label : labels) {
            if (!label.matches("[A-F]")) {
                throw new IllegalArgumentException(
                    "Đáp án đúng không hợp lệ: '" + label + "'. Chỉ chấp nhận A-F"
                );
            }
        }

        // Validate theo loại câu hỏi
        if (type == QuestionType.SINGLE_CHOICE && labels.size() != 1) {
            throw new IllegalArgumentException(
                "Câu SINGLE_CHOICE chỉ được có 1 đáp án đúng, nhưng nhận được: " + input
            );
        }

        if (type == QuestionType.MULTIPLE_CHOICE && labels.size() < 2) {
            throw new IllegalArgumentException(
                "Câu MULTIPLE_CHOICE phải có ít nhất 2 đáp án đúng"
            );
        }

        return labels;
    }

    /**
     * Validate câu hỏi trước khi lưu
     */
    private void validateQuestion(Question question) {
        int totalOptions = question.getTotalOptions();
        long correctCount = question.getCorrectAnswerCount();

        if (totalOptions < 2) {
            throw new IllegalArgumentException("Câu hỏi phải có ít nhất 2 đáp án");
        }

        if (totalOptions > 6) {
            throw new IllegalArgumentException("Câu hỏi chỉ được phép tối đa 6 đáp án (A-F)");
        }

        if (question.getType() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new IllegalArgumentException(
                "Câu SINGLE_CHOICE phải có đúng 1 đáp án đúng (hiện có: " + correctCount + ")"
            );
        }

        if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
            if (correctCount < 2) {
                throw new IllegalArgumentException(
                    "Câu MULTIPLE_CHOICE phải có ít nhất 2 đáp án đúng"
                );
            }
            if (correctCount >= totalOptions) {
                throw new IllegalArgumentException(
                    "Câu MULTIPLE_CHOICE không được có TẤT CẢ đáp án đều đúng. " +
                    "Số đáp án đúng (" + correctCount + ") phải < tổng số đáp án (" + totalOptions + ")"
                );
            }
        }
    }

    /**
     * Lấy giá trị cell dưới dạng String
     */
    private String getCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
