package vn.edu.aros.aroscore.service.omr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.aros.aroscore.dto.matrix.AnswerMapping;
import vn.edu.aros.aroscore.dto.matrix.QuestionMatrix;
import vn.edu.aros.aroscore.dto.omr.OmrScanResponse;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.ExamQuestion;
import vn.edu.aros.aroscore.entity.ExamVersion;
import vn.edu.aros.aroscore.repository.ExamVersionRepository;

import java.util.*;

@Component
@RequiredArgsConstructor
public class OmrGradingService {

    private final ExamVersionRepository examVersionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<Integer, String> buildAnswerKey(Exam exam, String versionCode) {
        ExamVersion version = examVersionRepository
                .findByExamIdAndVersionCode(exam.getId(), versionCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã đề \"" + versionCode + "\" không tồn tại trong đề thi này!"));

        List<QuestionMatrix> matrixList = readMatrix(version.getShuffleMatrix());
        matrixList.sort(Comparator.comparingInt(QuestionMatrix::getNewOrder));

        Map<Integer, String> answerKey = new LinkedHashMap<>();
        for (QuestionMatrix qm : matrixList) {
            String correct = qm.getAnswerMappings().stream()
                    .filter(am -> Boolean.TRUE.equals(am.getIsCorrect()))
                    .map(AnswerMapping::getNewLabel)
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            answerKey.put(qm.getNewOrder(), correct);
        }
        return answerKey;
    }

    public GradingOutcome grade(
            Exam exam,
            String versionCode,
            OmrScanResponse scan,
            List<Integer> engineNeedReview) {

        Map<Integer, String> answerKey = buildAnswerKey(exam, versionCode);

        Map<String, OmrScanResponse.OmrAnswerItem> items = scan.getAnswers() != null
                && scan.getAnswers().getItems() != null
                ? scan.getAnswers().getItems()
                : Collections.emptyMap();

        double totalRaw = 0;
        double earnedRaw = 0;
        List<GradedAnswer> graded = new ArrayList<>();

        Map<Long, Double> rawByQuestionId = new HashMap<>();
        if (exam.getExamQuestions() != null) {
            for (ExamQuestion eq : exam.getExamQuestions()) {
                rawByQuestionId.put(eq.getQuestion().getId(), eq.getRawPoint());
            }
        }

        ExamVersion version = examVersionRepository
                .findByExamIdAndVersionCode(exam.getId(), versionCode)
                .orElseThrow();
        Map<Integer, Long> orderToQuestionId = new HashMap<>();
        for (QuestionMatrix qm : readMatrix(version.getShuffleMatrix())) {
            orderToQuestionId.put(qm.getNewOrder(), qm.getOriginalQuestionId());
        }

        // Chỉ chấm đúng số câu của đề (answer key)
        for (Map.Entry<Integer, String> entry : answerKey.entrySet()) {
            int questionNum = entry.getKey();
            String correctAnswer = entry.getValue();
            OmrScanResponse.OmrAnswerItem item = items.get(String.valueOf(questionNum));

            String chosen = item != null ? item.getChosen() : null;
            String status = item != null && item.getStatus() != null ? item.getStatus() : "BLANK";
            String level = resolveLevel(item);

            boolean isCorrect = false;
            if (chosen != null && !chosen.isBlank() && !"X".equalsIgnoreCase(chosen)
                    && !"BLANK".equalsIgnoreCase(status)
                    && !"DUPLICATE".equalsIgnoreCase(status)
                    && !"DETECTION_ERROR".equalsIgnoreCase(status)
                    && !"MULTI_MARK".equalsIgnoreCase(status)
                    && !"error".equals(level)
                    && !"empty".equals(level)) {
                isCorrect = chosen.equalsIgnoreCase(correctAnswer);
            }
            if ("warning".equals(level)) {
                level = isCorrect ? "ok" : "error";
            }

            Long questionId = orderToQuestionId.get(questionNum);
            double raw = questionId != null ? rawByQuestionId.getOrDefault(questionId, 1.0) : 1.0;
            totalRaw += raw;
            if (isCorrect) {
                earnedRaw += raw;
            }

            String bubbleJson = toJsonQuietly(item != null ? item.getBubble() : null);
            String overlayJson = toJsonQuietly(buildOverlayBubbles(item, correctAnswer));

            graded.add(new GradedAnswer(
                    questionNum, chosen, correctAnswer, isCorrect, status, level,
                    isCorrect ? "green" : "red",
                    bubbleJson, overlayJson, true));
        }

        List<Integer> needReview = graded.stream()
                .filter(GradedAnswer::needsReview)
                .map(GradedAnswer::questionNumber)
                .toList();

        double maxScore = exam.getMaxScore() != null ? exam.getMaxScore() : 10.0;
        double score = totalRaw > 0 ? (earnedRaw / totalRaw) * maxScore : 0;

        return new GradingOutcome(score, maxScore, graded, needReview);
    }

    public GradingOutcome regradeManual(
            Exam exam,
            String versionCode,
            Map<Integer, String> manualAnswers,
            Map<Integer, String> preservedBubbleJson,
            Map<Integer, String> preservedOverlayJson) {
        Map<Integer, String> answerKey = buildAnswerKey(exam, versionCode);
        double totalRaw = 0;
        double earnedRaw = 0;
        List<GradedAnswer> graded = new ArrayList<>();

        Map<Long, Double> rawByQuestionId = new HashMap<>();
        if (exam.getExamQuestions() != null) {
            for (ExamQuestion eq : exam.getExamQuestions()) {
                rawByQuestionId.put(eq.getQuestion().getId(), eq.getRawPoint());
            }
        }
        ExamVersion version = examVersionRepository
                .findByExamIdAndVersionCode(exam.getId(), versionCode)
                .orElseThrow();
        Map<Integer, Long> orderToQuestionId = new HashMap<>();
        for (QuestionMatrix qm : readMatrix(version.getShuffleMatrix())) {
            orderToQuestionId.put(qm.getNewOrder(), qm.getOriginalQuestionId());
        }

        for (Map.Entry<Integer, String> entry : answerKey.entrySet()) {
            int questionNum = entry.getKey();
            String correctAnswer = entry.getValue();
            String chosen = manualAnswers.get(questionNum);
            boolean isCorrect = chosen != null && chosen.equalsIgnoreCase(correctAnswer);
            Long questionId = orderToQuestionId.get(questionNum);
            double raw = questionId != null ? rawByQuestionId.getOrDefault(questionId, 1.0) : 1.0;
            totalRaw += raw;
            if (isCorrect) {
                earnedRaw += raw;
            }
            String bubbleJson = preservedBubbleJson != null ? preservedBubbleJson.get(questionNum) : null;
            String overlayJson = preservedOverlayJson != null ? preservedOverlayJson.get(questionNum) : null;
            graded.add(new GradedAnswer(
                    questionNum, chosen, correctAnswer, isCorrect, "MANUAL", "ok", "green",
                    bubbleJson, overlayJson, false));
        }

        double maxScore = exam.getMaxScore() != null ? exam.getMaxScore() : 10.0;
        double score = totalRaw > 0 ? (earnedRaw / totalRaw) * maxScore : 0;
        return new GradingOutcome(score, maxScore, graded, List.of());
    }

    private List<QuestionMatrix> readMatrix(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionMatrix.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi đọc ma trận mã đề!");
        }
    }

    /** Engine mới gửi `level`; engine cũ chỉ có `status`. */
    static String resolveLevel(OmrScanResponse.OmrAnswerItem item) {
        if (item == null) {
            return "empty";
        }
        if (item.getLevel() != null && !item.getLevel().isBlank()) {
            return item.getLevel().trim().toLowerCase(java.util.Locale.ROOT);
        }
        String status = item.getStatus() != null ? item.getStatus().trim().toUpperCase(java.util.Locale.ROOT) : "";
        return switch (status) {
            case "OK" -> "ok";
            case "UNCLEAR", "AMBIGUOUS", "MULTI_FAINT", "GLARE", "LOW_CONFIDENCE" -> "warning";
            case "MULTI_MARK", "DUPLICATE", "DETECTION_ERROR" -> "error";
            case "BLANK" -> "empty";
            default -> {
                String chosen = item.getChosen();
                if (chosen == null || chosen.isBlank()) {
                    yield "empty";
                }
                if ("X".equalsIgnoreCase(chosen)) {
                    yield "error";
                }
                yield "ok";
            }
        };
    }

    /**
     * Chỉ overlay ô SV đã tô (không vẽ đáp án đúng của đề nếu không chọn).
     * xanh = tô đúng; đỏ = tô sai / tô trùng; trống = không vẽ.
     */
    static List<Map<String, Object>> buildOverlayBubbles(
            OmrScanResponse.OmrAnswerItem item, String correctAnswer) {
        if (item == null) {
            return List.of();
        }
        String chosen = item.getChosen();
        String level = resolveLevel(item);
        if ("empty".equals(level)
                || chosen == null
                || chosen.isBlank()
                || "BLANK".equalsIgnoreCase(item.getStatus())) {
            return List.of();
        }

        Set<String> filled = new LinkedHashSet<>();
        if (item.getMarkedBubbles() != null) {
            for (OmrScanResponse.OmrBubble marked : item.getMarkedBubbles()) {
                if (marked.getChoice() != null && !marked.getChoice().isBlank()) {
                    filled.add(marked.getChoice().trim().toUpperCase(java.util.Locale.ROOT));
                }
            }
        }
        if (item.getBubbles() != null) {
            for (OmrScanResponse.OmrBubble b : item.getBubbles()) {
                if (Boolean.TRUE.equals(b.getSelected()) && b.getChoice() != null) {
                    filled.add(b.getChoice().trim().toUpperCase(java.util.Locale.ROOT));
                }
            }
        }
        boolean multi = filled.size() >= 2
                || "X".equalsIgnoreCase(chosen)
                || "MULTI_MARK".equalsIgnoreCase(item.getStatus());
        if (!multi) {
            filled.clear();
            filled.add(chosen.trim().toUpperCase(java.util.Locale.ROOT));
        } else if (filled.isEmpty() && item.getBubble() != null && item.getBubble().getChoice() != null) {
            filled.add(item.getBubble().getChoice().trim().toUpperCase(java.util.Locale.ROOT));
        }
        if (filled.isEmpty()) {
            return List.of();
        }

        String correct = correctAnswer != null ? correctAnswer.trim().toUpperCase(java.util.Locale.ROOT) : null;
        List<Map<String, Object>> overlays = new ArrayList<>();
        for (String choice : filled) {
            OmrScanResponse.OmrBubble b = findBubble(item, choice);
            if (b == null || b.getW() == null || b.getH() == null || b.getW() <= 0 || b.getH() <= 0) {
                continue;
            }
            String tone;
            if (filled.size() > 1) {
                tone = "red";
            } else if (correct != null && choice.equals(correct)) {
                tone = "green";
            } else {
                tone = "red";
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("choice", b.getChoice() != null ? b.getChoice() : choice);
            row.put("x", b.getX());
            row.put("y", b.getY());
            row.put("w", b.getW());
            row.put("h", b.getH());
            row.put("tone", tone);
            overlays.add(row);
        }
        return overlays;
    }

    static OmrScanResponse.OmrBubble findBubble(OmrScanResponse.OmrAnswerItem item, String choice) {
        if (item.getBubbles() != null) {
            for (OmrScanResponse.OmrBubble b : item.getBubbles()) {
                if (b.getChoice() != null && b.getChoice().equalsIgnoreCase(choice)) {
                    return b;
                }
            }
        }
        if (item.getMarkedBubbles() != null) {
            for (OmrScanResponse.OmrBubble b : item.getMarkedBubbles()) {
                if (b.getChoice() != null && b.getChoice().equalsIgnoreCase(choice)) {
                    return b;
                }
            }
        }
        if (item.getBubble() != null
                && item.getBubble().getChoice() != null
                && item.getBubble().getChoice().equalsIgnoreCase(choice)) {
            return item.getBubble();
        }
        return null;
    }

    static String resolveStatus(OmrScanResponse.OmrAnswerItem item, String level) {
        if (item != null && item.getStatus() != null && !item.getStatus().isBlank()) {
            return item.getStatus();
        }
        return switch (level) {
            case "ok" -> "OK";
            case "warning" -> "UNCLEAR";
            case "error" -> "MULTI_MARK";
            default -> "BLANK";
        };
    }

    static String resolveColor(OmrScanResponse.OmrAnswerItem item, String level) {
        if (item != null && item.getColor() != null && !item.getColor().isBlank()) {
            return item.getColor();
        }
        return switch (level) {
            case "ok" -> "green";
            case "warning" -> "yellow";
            case "error" -> "red";
            default -> "gray";
        };
    }

    private String toJsonQuietly(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    public record GradedAnswer(
            int questionNumber,
            String chosen,
            String correctAnswer,
            boolean isCorrect,
            String omrStatus,
            String level,
            String color,
            String bubbleJson,
            String markedBubblesJson,
            boolean needsReview) {}

    public record GradingOutcome(
            double score,
            double maxScore,
            List<GradedAnswer> answers,
            List<Integer> needReview) {}
}
