package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer duration; // Thời gian làm bài (phút)

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_mode", nullable = false)
    private ExamMode examMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamVersion> examVersions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Helper method để tiện add câu hỏi vào đề gốc
    public void addQuestion(Question question, Integer order) {
        ExamQuestion eq = ExamQuestion.builder()
                .exam(this)
                .question(question)
                .questionOrder(order)
                .build();
        this.examQuestions.add(eq);
    }
}