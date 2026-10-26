package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.response.AgentRunCreatedResponse;
import vn.edu.aros.aroscore.dto.response.AgentRunResponse;
import vn.edu.aros.aroscore.dto.response.ClassInsightResponse;

public interface ClassAnalysisAgentService {

    /** Tạo run phân tích lớp (PENDING). Gemini loop sẽ được bổ sung ở đoạn tiếp theo. */
    AgentRunCreatedResponse startRun(Long examId, Long classroomId);

    AgentRunResponse getRun(Long examId, Long classroomId, Long runId);

    ClassInsightResponse getLatestInsight(Long examId, Long classroomId);
}
