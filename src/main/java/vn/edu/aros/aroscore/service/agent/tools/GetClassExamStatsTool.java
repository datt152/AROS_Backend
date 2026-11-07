package vn.edu.aros.aroscore.service.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.aros.aroscore.dto.response.ExamStatsResponse;
import vn.edu.aros.aroscore.dto.response.ScoreBucketResponse;
import vn.edu.aros.aroscore.service.ExamService;
import vn.edu.aros.aroscore.service.agent.AgentTool;
import vn.edu.aros.aroscore.service.agent.AgentToolContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lấy thống kê lớp (ẩn danh) — không gồm danh sách sinh viên.
 * examId/classroomId lấy từ context, không tin args từ model.
 */
@Component
@RequiredArgsConstructor
public class GetClassExamStatsTool implements AgentTool {

    private final ExamService examService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_class_exam_stats";
    }

    @Override
    public String description() {
        return "Lấy thống kê tổng quan bài làm của lớp: số nộp, điểm trung bình/cao/thấp, phân bố điểm. "
                + "Không trả thông tin cá nhân sinh viên.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", Map.of());
        schema.put("required", java.util.List.of());
        return schema;
    }

    @Override
    public JsonNode execute(JsonNode args, AgentToolContext context) {
        ExamStatsResponse stats = examService.getExamStats(context.getExamId(), context.getClassroomId());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("examTitle", stats.getExamTitle());
        root.put("classroomName", stats.getClassroomName());
        root.put("maxScore", stats.getMaxScore() != null ? stats.getMaxScore() : 10.0);
        root.put("totalStudents", nullSafe(stats.getTotalStudents()));
        root.put("submittedCount", nullSafe(stats.getSubmittedCount()));
        root.put("inProgressCount", nullSafe(stats.getInProgressCount()));
        root.put("expiredCount", nullSafe(stats.getExpiredCount()));
        root.put("notStartedCount", nullSafe(stats.getNotStartedCount()));
        if (stats.getAverageScore() != null) {
            root.put("averageScore", stats.getAverageScore());
        } else {
            root.putNull("averageScore");
        }
        if (stats.getHighestScore() != null) {
            root.put("highestScore", stats.getHighestScore());
        } else {
            root.putNull("highestScore");
        }
        if (stats.getLowestScore() != null) {
            root.put("lowestScore", stats.getLowestScore());
        } else {
            root.putNull("lowestScore");
        }

        ArrayNode distribution = root.putArray("scoreDistribution");
        if (stats.getScoreDistribution() != null) {
            for (ScoreBucketResponse bucket : stats.getScoreDistribution()) {
                ObjectNode b = distribution.addObject();
                b.put("label", bucket.getLabel());
                b.put("count", nullSafe(bucket.getCount()));
            }
        }
        return root;
    }

    private static int nullSafe(Integer value) {
        return value != null ? value : 0;
    }
}
