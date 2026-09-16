package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;

/**
 * Cấu hình thi trực tuyến
 */
@Entity
@Table(name = "online_exam_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineExamSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "allow_edit")
    @Builder.Default
    private Boolean allowEdit = true;

    @Column(name = "show_score_to_student")
    @Builder.Default
    private Boolean showScoreToStudent = true;

    /** null = không giới hạn (PRACTICE)*/
    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "time_limit_enabled")
    @Builder.Default
    private Boolean timeLimitEnabled = true;
}
