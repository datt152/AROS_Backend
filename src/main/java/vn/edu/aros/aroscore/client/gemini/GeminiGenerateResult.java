package vn.edu.aros.aroscore.client.gemini;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record GeminiGenerateResult(
        List<FunctionCall> functionCalls,
        String text,
        JsonNode modelContent
) {
    public record FunctionCall(String name, JsonNode args) {
    }

    public boolean hasFunctionCalls() {
        return functionCalls != null && !functionCalls.isEmpty();
    }
}
