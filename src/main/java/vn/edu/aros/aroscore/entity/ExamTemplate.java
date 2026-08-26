package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_mode", nullable = false)
    private ExamMode examMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    @Builder.Default
    private ExamPurpose purpose = ExamPurpose.EXAM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Double maxScore = 10.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // --- config snapshot (copy sang ExamConfig khi tạo đề) ---
    @Column(length = 20)
    private String semester;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    @Builder.Default
    private ExamType examType = ExamType.ONLINE;

    @Column(name = "shuffle_questions")
    @Builder.Default
    private Boolean shuffleQuestions = true;

    @Column(name = "shuffle_answers")
    @Builder.Default
    private Boolean shuffleAnswers = true;

    @Column(name = "paper_count")
    @Builder.Default
    private Integer paperCount = 1;

    @Column(name = "allow_edit")
    @Builder.Default
    private Boolean allowEdit = true;

    @Column(name = "show_score_to_student")
    @Builder.Default
    private Boolean showScoreToStudent = true;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "time_limit_enabled")
    @Builder.Default
    private Boolean timeLimitEnabled = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamTemplateQuestion> templateQuestions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.purpose == null) {
            this.purpose = ExamPurpose.EXAM;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    public void addQuestion(Question question, Integer order, Double rawPoint) {
        ExamTemplateQuestion tq = ExamTemplateQuestion.builder()
                .template(this)
                .question(question)
                .questionOrder(order)
                .rawPoint(rawPoint)
                .build();
        this.templateQuestions.add(tq);
    }
}
