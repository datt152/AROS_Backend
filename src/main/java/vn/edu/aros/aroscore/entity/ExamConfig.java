package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.ExamType;

@Entity
@Table(name = "exam_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa ngoại trỏ ngược lại bảng Exam (Bảng này giữ khóa)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    @Column(length = 20)
    private String semester;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType = ExamType.ONLINE;

    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = false;

    @Column(name = "shuffle_answers")
    private Boolean shuffleAnswers = false;

    @Column(name = "paper_count")
    private Integer paperCount = 1;

    @Column(name = "allow_edit")
    private Boolean allowEdit = true;
}