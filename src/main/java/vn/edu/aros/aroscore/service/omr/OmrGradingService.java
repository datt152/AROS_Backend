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
                .orElseThrow(() -> new RuntimeException("Mã đề \"" + versionCode + "\" không tồn tại!"));

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
        Set<Integer> needReview = new LinkedHashSet<>();
        if (engineNeedReview != null) {
            needReview.addAll(engineNeedReview);
        }

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
            OmrScanResponse.OmrAnswerItem item = items.get(String.valueOf(questionNum));

            String chosen = item != null ? item.getChosen() : null;
            String status = item != null ? item.getStatus() : "BLANK";
            boolean review = needReview.contains(questionNum);
            boolean isCorrect = false;

            if (chosen == null || chosen.isBlank() || "X".equalsIgnoreCase(chosen)) {
                review = true;
            } else if ("BLANK".equals(status) || "DUPLICATE".equals(status) || "DETECTION_ERROR".equals(status)) {
                review = true;
            } else if ("LOW_CONFIDENCE".equals(status)) {
                review = true;
                isCorrect = chosen.equalsIgnoreCase(correctAnswer);
            } else {
                isCorrect = chosen.equalsIgnoreCase(correctAnswer);
            }

            Long questionId = orderToQuestionId.get(questionNum);
            double raw = questionId != null ? rawByQuestionId.getOrDefault(questionId, 1.0) : 1.0;
            totalRaw += raw;
            if (isCorrect) {
                earnedRaw += raw;
            }

            String bubbleJson = null;
            if (item != null && item.getBubble() != null) {
                try {
                    bubbleJson = objectMapper.writeValueAsString(item.getBubble());
                } catch (JsonProcessingException ignored) {
                }
            }

            graded.add(new GradedAnswer(
                    questionNum, chosen, correctAnswer, isCorrect, status, bubbleJson, review));
        }

        double maxScore = exam.getMaxScore() != null ? exam.getMaxScore() : 10.0;
        double score = totalRaw > 0 ? (earnedRaw / totalRaw) * maxScore : 0;

        return new GradingOutcome(score, maxScore, graded, new ArrayList<>(needReview));
    }

    public GradingOutcome regradeManual(Exam exam, String versionCode, Map<Integer, String> manualAnswers) {
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
            graded.add(new GradedAnswer(
                    questionNum, chosen, correctAnswer, isCorrect, "MANUAL", null, false));
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

    public record GradedAnswer(
            int questionNumber,
            String chosen,
            String correctAnswer,
            boolean isCorrect,
            String omrStatus,
            String bubbleJson,
            boolean needsReview) {}

    public record GradingOutcome(
            double score,
            double maxScore,
            List<GradedAnswer> answers,
            List<Integer> needReview) {}
}
