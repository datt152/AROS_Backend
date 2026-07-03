package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exam_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "total_score")
    private Double totalScore;

    // Bài làm này của kỳ thi nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // Sinh viên nào làm bài
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Sinh viên làm mã đề nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_paper_id")
    private ExamPaper examPaper;

    // Chi tiết từng câu trả lời của sinh viên
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL)
    private List<SubmissionDetail> details;
}