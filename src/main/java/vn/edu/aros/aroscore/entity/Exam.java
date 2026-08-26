package vn.edu.aros.aroscore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** EXAM = kỳ thi chính thức; PRACTICE = bài luyện tập. */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    @Builder.Default
    private ExamPurpose purpose = ExamPurpose.EXAM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnore
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnore
    private User teacher;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamVersion> examVersions = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "exam_classrooms",
            joinColumns = @JoinColumn(name = "exam_id"),
            inverseJoinColumns = @JoinColumn(name = "classroom_id")
    )
    @Builder.Default
    private Set<Classroom> classrooms = new HashSet<>();

    @OneToOne(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ExamConfig config;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ExamStatus.DRAFT;
        }
        if (this.purpose == null) {
            this.purpose = ExamPurpose.EXAM;
        }
    }

    private String imageUrl;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Double maxScore = 10.0;

    public void addQuestion(Question question, Integer order, Double rawPoint) {
        ExamQuestion eq = ExamQuestion.builder()
                .exam(this)
                .question(question)
                .questionOrder(order)
                .rawPoint(rawPoint)
                .build();
        this.examQuestions.add(eq);
    }
}
