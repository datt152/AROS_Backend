package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.AgentRunStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_runs", indexes = {
        @Index(name = "idx_agent_run_exam_classroom", columnList = "exam_id, classroom_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    /** Email GV tạo run (đối chiếu quyền). */
    @Column(name = "teacher_email", nullable = false, length = 255)
    private String teacherEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AgentRunStatus status = AgentRunStatus.PENDING;

    @Column(nullable = false, length = 500)
    @Builder.Default
    private String goal = "Phân tích kết quả làm bài của lớp và đưa ra nhận xét, gợi ý giảng dạy.";

    /** Báo cáo cuối (JSON string). */
    @Column(name = "final_report", columnDefinition = "TEXT")
    private String finalReport;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNo ASC")
    @Builder.Default
    private List<AgentStep> steps = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AgentRunStatus.PENDING;
        }
    }

    public void addStep(AgentStep step) {
        steps.add(step);
        step.setRun(this);
    }
}
