package vn.edu.aros.aroscore.service.agent;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolsByName;

    public AgentToolRegistry(List<AgentTool> tools) {
        Map<String, AgentTool> map = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            map.put(tool.name(), tool);
        }
        this.toolsByName = Map.copyOf(map);
    }

    public AgentTool require(String name) {
        AgentTool tool = toolsByName.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Không hỗ trợ tool: " + name);
        }
        return tool;
    }

    public Collection<AgentTool> all() {
        return toolsByName.values();
    }
}
