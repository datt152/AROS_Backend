package vn.edu.aros.aroscore.service.agent;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentToolContext {
    private final Long runId;
    private final Long examId;
    private final Long classroomId;
    private final String teacherEmail;
}
