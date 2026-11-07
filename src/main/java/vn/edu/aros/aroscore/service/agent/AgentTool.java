package vn.edu.aros.aroscore.service.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface AgentTool {

    String name();

    String description();

    /** JSON Schema properties cho Gemini functionDeclarations.parameters.properties */
    Map<String, Object> parameterSchema();

    JsonNode execute(JsonNode args, AgentToolContext context) throws Exception;
}
