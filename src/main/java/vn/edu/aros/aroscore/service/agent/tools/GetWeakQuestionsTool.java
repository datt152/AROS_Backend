package vn.edu.aros.aroscore.service.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.aros.aroscore.dto.response.ExamStatsResponse;
import vn.edu.aros.aroscore.dto.response.QuestionStatsResponse;
import vn.edu.aros.aroscore.service.ExamService;
import vn.edu.aros.aroscore.service.agent.AgentTool;
import vn.edu.aros.aroscore.service.agent.AgentToolContext;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetWeakQuestionsTool implements AgentTool {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 10;
    private static final int CONTENT_MAX_LEN = 220;

    private final ExamService examService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_weak_questions";
    }

    @Override
    public String description() {
        return "Lấy các câu hỏi lớp làm kém nhất theo correctRate (tỉ lệ đúng). "
                + "Nội dung câu được rút gọn, không kèm bài làm từng sinh viên.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> topN = new LinkedHashMap<>();
        topN.put("type", "INTEGER");
        topN.put("description", "Số câu yếu cần lấy (mặc định 5, tối đa 10)");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("topN", topN);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", List.of());
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode args, AgentToolContext context) {
        int topN = DEFAULT_TOP_N;
        if (args != null && args.has("topN") && args.get("topN").canConvertToInt()) {
            topN = Math.min(MAX_TOP_N, Math.max(1, args.get("topN").asInt()));
        }

        ExamStatsResponse stats = examService.getExamStats(context.getExamId(), context.getClassroomId());
        List<QuestionStatsResponse> questions = stats.getQuestionStats() == null
                ? List.of()
                : stats.getQuestionStats().stream()
                .sorted(Comparator.comparing(
                        q -> q.getCorrectRate() != null ? q.getCorrectRate() : 1.0))
                .limit(topN)
                .toList();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("topN", topN);
        ArrayNode items = root.putArray("weakQuestions");
        for (QuestionStatsResponse q : questions) {
            ObjectNode item = items.addObject();
            item.put("order", q.getOrder() != null ? q.getOrder() : 0);
            item.put("difficulty", q.getDifficulty() != null ? q.getDifficulty() : "");
            item.put("content", truncate(q.getContent()));
            item.put("answeredCount", q.getAnsweredCount() != null ? q.getAnsweredCount() : 0);
            item.put("correctCount", q.getCorrectCount() != null ? q.getCorrectCount() : 0);
            if (q.getCorrectRate() != null) {
                item.put("correctRate", q.getCorrectRate());
            } else {
                item.putNull("correctRate");
            }
        }
        return root;
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= CONTENT_MAX_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, CONTENT_MAX_LEN) + "…";
    }
}
