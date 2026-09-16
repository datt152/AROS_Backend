package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Cấu hình in đề / mã đề giấy — dùng cho OMR và (tuỳ chọn) Online khi sinh phiên bản.
 */
@Entity
@Table(name = "paper_exam_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperExamSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    /** Ngày thi (in lên đề). */
    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(length = 20)
    private String semester;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "shuffle_questions")
    @Builder.Default
    private Boolean shuffleQuestions = true;

    @Column(name = "shuffle_answers")
    @Builder.Default
    private Boolean shuffleAnswers = true;

    /** Số mã đề / phiên bản khi generate. */
    @Column(name = "paper_count")
    @Builder.Default
    private Integer paperCount = 1;
}
