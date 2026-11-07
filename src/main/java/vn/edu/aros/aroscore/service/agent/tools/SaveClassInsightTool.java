package vn.edu.aros.aroscore.service.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.entity.AgentRun;
import vn.edu.aros.aroscore.entity.ClassInsight;
import vn.edu.aros.aroscore.repository.AgentRunRepository;
import vn.edu.aros.aroscore.repository.ClassInsightRepository;
import vn.edu.aros.aroscore.service.agent.AgentTool;
import vn.edu.aros.aroscore.service.agent.AgentToolContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lưu báo cáo cuối. Gọi tool này = agent kết thúc thành công.
 */
@Component
@RequiredArgsConstructor
public class SaveClassInsightTool implements AgentTool {

    private final AgentRunRepository agentRunRepository;
    private final ClassInsightRepository classInsightRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "save_class_insight";
    }

    @Override
    public String description() {
        return "Lưu nhận xét cuối cho lớp sau khi đã đủ dữ liệu. "
                + "Bắt buộc truyền summary (string), weaknesses (array), suggestions (array of string). "
                + "Chỉ gọi khi đã lấy thống kê và câu yếu.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> summary = Map.of(
                "type", "STRING",
                "description", "Tóm tắt nhận xét lớp bằng tiếng Việt");

        Map<String, Object> weaknessItem = new LinkedHashMap<>();
        weaknessItem.put("type", "OBJECT");
        weaknessItem.put("properties", Map.of(
                "order", Map.of("type", "INTEGER", "description", "Số thứ tự câu hỏi"),
                "correctRate", Map.of("type", "NUMBER", "description", "Tỉ lệ đúng 0-1"),
                "note", Map.of("type", "STRING", "description", "Giải thích ngắn")));

        Map<String, Object> weaknesses = new LinkedHashMap<>();
        weaknesses.put("type", "ARRAY");
        weaknesses.put("description", "Danh sách điểm yếu / câu hỏi yếu");
        weaknesses.put("items", weaknessItem);

        Map<String, Object> suggestions = new LinkedHashMap<>();
        suggestions.put("type", "ARRAY");
        suggestions.put("description", "Gợi ý giảng dạy / ôn tập");
        suggestions.put("items", Map.of("type", "STRING"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("summary", summary);
        properties.put("weaknesses", weaknesses);
        properties.put("suggestions", suggestions);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", List.of("summary", "weaknesses", "suggestions"));
        return schema;
    }

    @Override
    @Transactional
    public JsonNode execute(JsonNode args, AgentToolContext context) throws Exception {
        if (args == null || !args.hasNonNull("summary")) {
            throw new IllegalArgumentException("save_class_insight cần trường summary");
        }

        ObjectNode report = objectMapper.createObjectNode();
        report.put("summary", args.get("summary").asText());
        report.set("weaknesses", args.has("weaknesses") ? args.get("weaknesses") : objectMapper.createArrayNode());
        report.set("suggestions", args.has("suggestions") ? args.get("suggestions") : objectMapper.createArrayNode());

        String reportJson = objectMapper.writeValueAsString(report);

        AgentRun run = agentRunRepository.findById(context.getRunId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy agent run #" + context.getRunId()));

        ClassInsight insight = ClassInsight.builder()
                .exam(run.getExam())
                .classroom(run.getClassroom())
                .reportJson(reportJson)
                .sourceRun(run)
                .build();
        classInsightRepository.save(insight);

        run.setFinalReport(reportJson);
        agentRunRepository.save(run);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("saved", true);
        result.put("insightId", insight.getId());
        return result;
    }
}
