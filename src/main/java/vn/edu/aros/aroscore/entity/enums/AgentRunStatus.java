package vn.edu.aros.aroscore.entity.enums;

public enum AgentRunStatus {
    /** Đã tạo, chờ orchestrator (Gemini loop) xử lý. */
    PENDING,
    /** Đang gọi LLM / tool. */
    RUNNING,
    DONE,
    FAILED
}
