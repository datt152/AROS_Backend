package vn.edu.aros.aroscore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.response.AgentRunCreatedResponse;
import vn.edu.aros.aroscore.dto.response.AgentRunResponse;
import vn.edu.aros.aroscore.dto.response.ClassInsightResponse;
import vn.edu.aros.aroscore.service.ClassAnalysisAgentService;

/**
 * AI Agent phân tích lớp theo đề ONLINE.
 * Đoạn 1: tạo / xem run + lấy insight đã lưu.
 * Đoạn 2: Gemini tool-calling orchestrator.
 */
@RestController
@RequestMapping("/api/v1/exams/{examId}/classrooms/{classroomId}")
@RequiredArgsConstructor
public class ExamAnalysisAgentController {

    private final ClassAnalysisAgentService classAnalysisAgentService;

    @PostMapping("/analysis-agent/runs")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AgentRunCreatedResponse> startRun(
            @PathVariable Long examId,
            @PathVariable Long classroomId) {
        AgentRunCreatedResponse body = classAnalysisAgentService.startRun(examId, classroomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/analysis-agent/runs/{runId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AgentRunResponse> getRun(
            @PathVariable Long examId,
            @PathVariable Long classroomId,
            @PathVariable Long runId) {
        return ResponseEntity.ok(classAnalysisAgentService.getRun(examId, classroomId, runId));
    }

    @GetMapping("/insights/latest")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassInsightResponse> getLatestInsight(
            @PathVariable Long examId,
            @PathVariable Long classroomId) {
        return ResponseEntity.ok(classAnalysisAgentService.getLatestInsight(examId, classroomId));
    }
}
