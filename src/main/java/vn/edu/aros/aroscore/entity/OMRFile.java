package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.OmrSheetStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "omr_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OMRFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** URL/path ảnh gốc do Backend lưu. */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "warped_url", length = 500)
    private String warpedUrl;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id")
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "detected_student_id", length = 20)
    private String detectedStudentId;

    @Column(name = "detected_exam_code", length = 10)
    private String detectedExamCode;

    @Column(name = "score")
    private Double score;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "omr_raw", columnDefinition = "TEXT")
    private String omrRaw;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OmrSheetStatus status = OmrSheetStatus.PROCESSING;

    @Column(name = "need_review_json", columnDefinition = "TEXT")
    private String needReviewJson;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    /** Đồng bộ sang bảng submissions để dùng màn chấm đề hiện có. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @OneToMany(mappedBy = "omrFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OmrAnswerDetail> answerDetails = new ArrayList<>();

    @OneToMany(mappedBy = "omrFile", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OMRResult> results = new ArrayList<>();

    public void addAnswerDetail(OmrAnswerDetail detail) {
        answerDetails.add(detail);
        detail.setOmrFile(this);
    }

    @PrePersist
    protected void onCreate() {
        if (uploadTime == null) {
            uploadTime = LocalDateTime.now();
        }
        if (status == null) {
            status = OmrSheetStatus.PROCESSING;
        }
    }
}
