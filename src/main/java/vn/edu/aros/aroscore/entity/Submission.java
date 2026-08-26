package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_exam_student_attempt", columnNames = {"exam_id", "student_id", "attempt_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Lần làm thứ mấy (1-based). Kỳ thi thường = 1; PRACTICE có thể > 1. */
    @Column(name = "attempt_no", nullable = false)
    @Builder.Default
    private Integer attemptNo = 1;

    // Mã đề học sinh làm (Ví dụ: "101")
    @Column(name = "version_code")
    private String versionCode;

    @Column(name = "score")
    private Double score; // Điểm tổng kết (ví dụ 9.0)

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubmissionDetail> details = new ArrayList<>();

    // Helper method để thêm chi tiết bài làm
    public void addDetail(SubmissionDetail detail) {
        details.add(detail);
        detail.setSubmission(this);
    }
}