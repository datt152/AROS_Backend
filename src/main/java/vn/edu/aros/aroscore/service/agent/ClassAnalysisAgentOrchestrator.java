package vn.edu.aros.aroscore.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.aros.aroscore.client.gemini.GeminiClient;
import vn.edu.aros.aroscore.client.gemini.GeminiGenerateResult;
import vn.edu.aros.aroscore.config.AgentProperties;
import vn.edu.aros.aroscore.entity.AgentRun;
import vn.edu.aros.aroscore.entity.AgentStep;
import vn.edu.aros.aroscore.entity.enums.AgentRunStatus;
import vn.edu.aros.aroscore.repository.AgentRunRepository;
import vn.edu.aros.aroscore.repository.AgentStepRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassAnalysisAgentOrchestrator {

    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final AgentToolRegistry toolRegistry;
    private final GeminiClient geminiClient;
    private final AgentProperties agentProperties;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void executeAsync(Long runId) {
        try {
            execute(runId);
        } catch (Exception e) {
            log.error("Agent run #{} failed", runId, e);
            markFailed(runId, abbreviate(e.getMessage(), 900));
        }
    }

    private void execute(Long runId) {
        AgentRun bootstrap = agentRunRepository.findByIdWithExamAndClassroom(runId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy run #" + runId));

        String teacherEmail = bootstrap.getTeacherEmail();
        Long examId = bootstrap.getExam().getId();
        Long classroomId = bootstrap.getClassroom().getId();
        String goal = bootstrap.getGoal();

        markRunning(runId);

        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            setTeacherContext(teacherEmail);

            AgentToolContext context = AgentToolContext.builder()
                    .runId(runId)
                    .examId(examId)
                    .classroomId(classroomId)
                    .teacherEmail(teacherEmail)
                    .build();

            List<ObjectNode> contents = new ArrayList<>();
            contents.add(geminiClient.userText(
                    goal + "\nexamId=" + examId + ", classroomId=" + classroomId
                            + "\nHãy bắt đầu bằng get_class_exam_stats."));

            boolean savedInsight = false;
            int maxSteps = Math.max(1, agentProperties.getMaxSteps());

            for (int step = 1; step <= maxSteps; step++) {
                GeminiGenerateResult result = geminiClient.generate(contents);

                if (!result.hasFunctionCalls()) {
                    if (savedInsight) {
                        markDone(runId);
                        return;
                    }
                    // Model trả text thay vì tool — nhắc gọi save
                    contents.add(geminiClient.modelContentFrom(result.modelContent()));
                    contents.add(geminiClient.userText(
                            "Bạn phải gọi tool. Nếu đã đủ dữ liệu hãy gọi save_class_insight. "
                                    + "Không chỉ trả lời văn bản."));
                    continue;
                }

                contents.add(geminiClient.modelContentFrom(result.modelContent()));

                List<JsonNode> toolResults = new ArrayList<>();
                for (GeminiGenerateResult.FunctionCall call : result.functionCalls()) {
                    JsonNode toolResult = runToolAndPersistStep(step, call, context);
                    toolResults.add(toolResult);
                    if ("save_class_insight".equals(call.name())) {
                        savedInsight = true;
                    }
                }

                contents.add(geminiClient.functionResponses(result.functionCalls(), toolResults));

                if (savedInsight) {
                    markDone(runId);
                    return;
                }
            }

            markFailed(runId, "Agent vượt quá số bước cho phép (" + maxSteps
                    + ") mà chưa gọi save_class_insight.");
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private JsonNode runToolAndPersistStep(
            int stepNo, GeminiGenerateResult.FunctionCall call, AgentToolContext context) {
        AgentTool tool = toolRegistry.require(call.name());
        String argsJson;
        String resultJson;
        JsonNode resultNode;
        try {
            argsJson = objectMapper.writeValueAsString(
                    call.args() != null ? call.args() : objectMapper.createObjectNode());
            resultNode = tool.execute(call.args(), context);
            resultJson = objectMapper.writeValueAsString(resultNode);
        } catch (Exception e) {
            ObjectNode err = objectMapper.createObjectNode();
            err.put("error", abbreviate(e.getMessage(), 500));
            resultNode = err;
            try {
                argsJson = objectMapper.writeValueAsString(
                        call.args() != null ? call.args() : objectMapper.createObjectNode());
                resultJson = objectMapper.writeValueAsString(err);
            } catch (Exception ignored) {
                argsJson = "{}";
                resultJson = "{\"error\":\"tool failed\"}";
            }
        }

        final String finalArgs = argsJson;
        final String finalResult = resultJson;
        transactionTemplate.executeWithoutResult(status -> {
            AgentRun run = agentRunRepository.findById(context.getRunId())
                    .orElseThrow(() -> new IllegalStateException("Run không tồn tại"));
            AgentStep step = AgentStep.builder()
                    .run(run)
                    .stepNo(stepNo)
                    .toolName(call.name())
                    .toolArgsJson(finalArgs)
                    .toolResultJson(truncateJson(finalResult, 8000))
                    .build();
            agentStepRepository.save(step);
        });
        return resultNode;
    }

    private void markRunning(Long runId) {
        transactionTemplate.executeWithoutResult(status -> {
            AgentRun run = agentRunRepository.findById(runId).orElseThrow();
            run.setStatus(AgentRunStatus.RUNNING);
            run.setErrorMessage(null);
            agentRunRepository.save(run);
        });
    }

    private void markDone(Long runId) {
        transactionTemplate.executeWithoutResult(status -> {
            AgentRun run = agentRunRepository.findById(runId).orElseThrow();
            run.setStatus(AgentRunStatus.DONE);
            run.setFinishedAt(LocalDateTime.now());
            run.setErrorMessage(null);
            agentRunRepository.save(run);
        });
    }

    private void markFailed(Long runId, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            agentRunRepository.findById(runId).ifPresent(run -> {
                run.setStatus(AgentRunStatus.FAILED);
                run.setFinishedAt(LocalDateTime.now());
                run.setErrorMessage(message);
                agentRunRepository.save(run);
            });
        });
    }

    private void setTeacherContext(String teacherEmail) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                teacherEmail,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
        SecurityContextHolder.setContext(context);
    }

    private static String truncateJson(String json, int max) {
        if (json == null || json.length() <= max) {
            return json;
        }
        return json.substring(0, max) + "…";
    }

    private static String abbreviate(String value, int max) {
        if (value == null) {
            return "Unknown error";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
