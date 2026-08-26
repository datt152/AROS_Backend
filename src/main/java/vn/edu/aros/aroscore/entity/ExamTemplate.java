package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thư viện đề = gói câu hỏi tái sử dụng (title + môn + câu).
 * Config / lịch / lớp / purpose thuộc Exam khi tạo đợt thi.
 */
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamTemplateQuestion> templateQuestions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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
