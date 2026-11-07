package vn.edu.aros.aroscore.client.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import vn.edu.aros.aroscore.config.GeminiProperties;
import vn.edu.aros.aroscore.service.agent.AgentTool;
import vn.edu.aros.aroscore.service.agent.AgentToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiGenerateResult generate(List<ObjectNode> contents) {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY");
        }

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contentsNode = body.putArray("contents");
        for (ObjectNode content : contents) {
            contentsNode.add(content);
        }
        body.set("tools", buildToolsNode());
        body.set("toolConfig", objectMapper.createObjectNode()
                .set("functionCallingConfig", objectMapper.createObjectNode()
                        .put("mode", "AUTO")));
        body.set("systemInstruction", objectMapper.createObjectNode()
                .set("parts", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("text", systemPrompt()))));

        String url = String.format("%s/models/%s:generateContent?key=%s",
                trimSlash(geminiProperties.getBaseUrl()),
                geminiProperties.getModel(),
                geminiProperties.getApiKey());

        try {
            // Gửi String JSON — tránh RestClient serialize ObjectNode như Java bean
            // (lỗi Gemini 400: Unknown name "array"/"bigDecimal"/...)
            String requestJson = objectMapper.writeValueAsString(body);
            int maxAttempts = Math.max(1, geminiProperties.getMaxRetries() + 1);
            RestClientResponseException lastHttpError = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    String responseJson = restClient().post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestJson)
                            .retrieve()
                            .body(String.class);
                    JsonNode response = objectMapper.readTree(responseJson);
                    return parseResponse(response);
                } catch (RestClientResponseException e) {
                    lastHttpError = e;
                    int status = e.getStatusCode().value();
                    log.warn("Gemini HTTP {} (attempt {}/{}): {}",
                            status, attempt, maxAttempts, abbreviate(e.getResponseBodyAsString(), 300));
                    if (!isRetryable(status) || attempt == maxAttempts) {
                        break;
                    }
                    sleepBeforeRetry(attempt);
                }
            }

            if (lastHttpError != null) {
                throw new IllegalStateException("Gọi Gemini thất bại (HTTP "
                        + lastHttpError.getStatusCode().value() + "): "
                        + abbreviate(lastHttpError.getResponseBodyAsString(), 400));
            }
            throw new IllegalStateException("Gọi Gemini thất bại không rõ nguyên nhân");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Gọi Gemini thất bại: " + e.getMessage(), e);
        }
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status == 503;
    }

    private void sleepBeforeRetry(int attempt) {
        long delayMs = Math.max(500L, (long) geminiProperties.getRetryDelayMs() * attempt);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị gián đoạn khi chờ retry Gemini");
        }
    }

    public ObjectNode userText(String text) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        content.putArray("parts").add(objectMapper.createObjectNode().put("text", text));
        return content;
    }

    public ObjectNode modelContentFrom(JsonNode modelContent) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "model");
        content.set("parts", modelContent.path("parts").deepCopy());
        return content;
    }

    public ObjectNode functionResponses(List<GeminiGenerateResult.FunctionCall> calls, List<JsonNode> results) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = content.putArray("parts");
        for (int i = 0; i < calls.size(); i++) {
            ObjectNode part = parts.addObject();
            ObjectNode fr = part.putObject("functionResponse");
            fr.put("name", calls.get(i).name());
            // Gemini expects "response" as a Struct (JSON object)
            JsonNode result = results.get(i);
            if (result != null && result.isObject()) {
                fr.set("response", result);
            } else {
                fr.set("response", objectMapper.createObjectNode().set("data", result));
            }
        }
        return content;
    }

    private ArrayNode buildToolsNode() {
        ArrayNode tools = objectMapper.createArrayNode();
        ObjectNode tool = tools.addObject();
        ArrayNode declarations = tool.putArray("functionDeclarations");
        for (AgentTool agentTool : toolRegistry.all()) {
            ObjectNode decl = declarations.addObject();
            decl.put("name", agentTool.name());
            decl.put("description", agentTool.description());
            Map<String, Object> schema = agentTool.parameterSchema();
            decl.set("parameters", objectMapper.valueToTree(schema));
        }
        return tools;
    }

    private GeminiGenerateResult parseResponse(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Gemini trả về body rỗng");
        }
        JsonNode candidate = response.path("candidates").path(0);
        if (candidate.isMissingNode()) {
            String block = response.path("promptFeedback").toString();
            throw new IllegalStateException("Gemini không trả candidate. feedback=" + abbreviate(block, 300));
        }
        JsonNode content = candidate.path("content");
        List<GeminiGenerateResult.FunctionCall> calls = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        JsonNode parts = content.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("functionCall")) {
                    JsonNode fc = part.get("functionCall");
                    String name = fc.path("name").asText();
                    JsonNode args = fc.path("args");
                    if (args.isMissingNode() || args.isNull()) {
                        args = objectMapper.createObjectNode();
                    }
                    calls.add(new GeminiGenerateResult.FunctionCall(name, args));
                }
                if (part.has("text")) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(part.path("text").asText());
                }
            }
        }
        return new GeminiGenerateResult(calls, text.toString().trim(), content);
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(geminiProperties.getConnectTimeoutMs());
        factory.setReadTimeout(geminiProperties.getReadTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }

    private static String systemPrompt() {
        return """
                Bạn là Teaching Analysis Agent của hệ thống AROS.
                Nhiệm vụ: phân tích kết quả làm bài ONLINE của một lớp và đưa nhận xét sư phạm bằng tiếng Việt.

                Quy tắc bắt buộc:
                1. Chỉ dùng số liệu từ kết quả tool. Không bịa điểm, tỉ lệ, số lượng.
                2. Không nêu tên, email, MSSV hay thông tin cá nhân sinh viên.
                3. Thứ tự gợi ý: get_class_exam_stats → get_weak_questions → save_class_insight.
                4. Khi đã đủ dữ liệu, phải gọi save_class_insight với summary, weaknesses, suggestions.
                5. Không trả lời dài ngoài tool call khi chưa lưu insight.
                """;
    }

    private static String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String abbreviate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
