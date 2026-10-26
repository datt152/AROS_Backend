package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.config.AgentProperties;
import vn.edu.aros.aroscore.config.GeminiProperties;
import vn.edu.aros.aroscore.dto.response.AgentRunCreatedResponse;
import vn.edu.aros.aroscore.dto.response.AgentRunResponse;
import vn.edu.aros.aroscore.dto.response.AgentStepResponse;
import vn.edu.aros.aroscore.dto.response.ClassInsightResponse;
import vn.edu.aros.aroscore.entity.AgentRun;
import vn.edu.aros.aroscore.entity.AgentStep;
import vn.edu.aros.aroscore.entity.ClassInsight;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.enums.AgentRunStatus;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.repository.AgentRunRepository;
import vn.edu.aros.aroscore.repository.AgentStepRepository;
import vn.edu.aros.aroscore.repository.ClassInsightRepository;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.SubmissionRepository;
import vn.edu.aros.aroscore.service.ClassAnalysisAgentService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassAnalysisAgentServiceImpl implements ClassAnalysisAgentService {

    private final ExamRepository examRepository;
    private final SubmissionRepository submissionRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final ClassInsightRepository classInsightRepository;
    private final AgentProperties agentProperties;
    private final GeminiProperties geminiProperties;

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public AgentRunCreatedResponse startRun(Long examId, Long classroomId) {
        String email = currentEmail();
        Exam exam = examRepository.findByIdAndTeacherEmail(examId, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi hoặc bạn không có quyền!"));

        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            throw new IllegalArgumentException("Agent phân tích hiện chỉ hỗ trợ đề ONLINE!");
        }

        Classroom classroom = resolveAssignedClassroom(exam, classroomId);

        long submitted = submissionRepository.countSubmittedByExamIdAndClassroomId(examId, classroomId);
        if (submitted < agentProperties.getMinSubmitted()) {
            throw new IllegalArgumentException(
                    "Cần ít nhất " + agentProperties.getMinSubmitted()
                            + " bài đã nộp trong lớp trước khi chạy Agent (hiện có " + submitted + ").");
        }

        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "Chưa cấu hình GEMINI_API_KEY. Thêm key vào .env rồi khởi động lại backend.");
        }

        AgentRun run = AgentRun.builder()
                .exam(exam)
                .classroom(classroom)
                .teacherEmail(email)
                .status(AgentRunStatus.PENDING)
                .goal("Phân tích kết quả làm bài của lớp \"" + classroom.getClassName()
                        + "\" cho đề \"" + exam.getTitle() + "\".")
                .build();
        agentRunRepository.save(run);

        // Đoạn 2: kích hoạt @Async Gemini tool-calling loop tại đây.
        return AgentRunCreatedResponse.builder()
                .runId(run.getId())
                .status(run.getStatus())
                .message("Đã tạo agent run. Orchestrator Gemini sẽ được nối ở bước tiếp theo.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentRunResponse getRun(Long examId, Long classroomId, Long runId) {
        String email = currentEmail();
        AgentRun run = agentRunRepository.findByIdAndTeacherEmail(runId, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy agent run hoặc bạn không có quyền!"));

        if (!run.getExam().getId().equals(examId) || !run.getClassroom().getId().equals(classroomId)) {
            throw new IllegalArgumentException("Agent run không thuộc đề/lớp đã chỉ định!");
        }

        List<AgentStep> steps = agentStepRepository.findAllByRunIdOrderByStepNoAsc(runId);
        return toRunResponse(run, steps);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassInsightResponse getLatestInsight(Long examId, Long classroomId) {
        String email = currentEmail();
        // Xác nhận GV sở hữu đề + lớp được giao
        Exam exam = examRepository.findByIdAndTeacherEmail(examId, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi hoặc bạn không có quyền!"));
        resolveAssignedClassroom(exam, classroomId);

        ClassInsight insight = classInsightRepository
                .findFirstByExam_IdAndClassroom_IdAndExam_Teacher_EmailOrderByCreatedAtDesc(
                        examId, classroomId, email)
                .orElseThrow(() -> new RuntimeException("Chưa có nhận xét Agent cho lớp này!"));

        return ClassInsightResponse.builder()
                .id(insight.getId())
                .examId(examId)
                .classroomId(classroomId)
                .classroomName(insight.getClassroom() != null ? insight.getClassroom().getClassName() : null)
                .reportJson(insight.getReportJson())
                .sourceRunId(insight.getSourceRun() != null ? insight.getSourceRun().getId() : null)
                .createdAt(insight.getCreatedAt())
                .build();
    }

    private Classroom resolveAssignedClassroom(Exam exam, Long classroomId) {
        if (exam.getClassrooms() == null || exam.getClassrooms().isEmpty()) {
            throw new IllegalArgumentException("Đề chưa được giao cho lớp nào!");
        }
        return exam.getClassrooms().stream()
                .filter(c -> c.getId().equals(classroomId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Lớp không thuộc đề thi này!"));
    }

    private AgentRunResponse toRunResponse(AgentRun run, List<AgentStep> steps) {
        return AgentRunResponse.builder()
                .id(run.getId())
                .examId(run.getExam().getId())
                .classroomId(run.getClassroom().getId())
                .classroomName(run.getClassroom().getClassName())
                .status(run.getStatus())
                .goal(run.getGoal())
                .finalReport(run.getFinalReport())
                .errorMessage(run.getErrorMessage())
                .createdAt(run.getCreatedAt())
                .finishedAt(run.getFinishedAt())
                .steps(steps.stream().map(this::toStepResponse).toList())
                .build();
    }

    private AgentStepResponse toStepResponse(AgentStep step) {
        return AgentStepResponse.builder()
                .id(step.getId())
                .stepNo(step.getStepNo())
                .thought(step.getThought())
                .toolName(step.getToolName())
                .toolArgsJson(step.getToolArgsJson())
                .toolResultJson(step.getToolResultJson())
                .createdAt(step.getCreatedAt())
                .build();
    }
}
