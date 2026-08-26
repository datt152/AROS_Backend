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

    /** Hiện điểm cho SV sau khi nộp (đặc biệt hữu ích với PRACTICE). */
    @Column(name = "show_score_to_student")
    @Builder.Default
    private Boolean showScoreToStudent = true;

    /**
     * Số lần làm tối đa (chỉ PRACTICE thực sự dùng).
     * null = không giới hạn với PRACTICE; EXAM luôn coi như 1 lần.
     */
    @Column(name = "max_attempts")
    private Integer maxAttempts;

    /**
     * true = áp dụng Exam.duration khi take/submit.
     * false = không giới hạn thời gian làm bài (thường dùng PRACTICE).
     */
    @Column(name = "time_limit_enabled")
    @Builder.Default
    private Boolean timeLimitEnabled = true;
}