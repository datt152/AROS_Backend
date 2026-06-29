package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;
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

    @Column(name = "exam_name", length = 200)
    private String examName;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ExamStatus status;

    // Kỳ thi của môn học nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    // Quan hệ 1-1: Mỗi kỳ thi có 1 cấu hình duy nhất
    @OneToOne(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ExamConfig config;

    // Quan hệ 1-N: Một kỳ thi có thể có nhiều mã đề (ExamPaper)
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL)
    private List<ExamPaper> examPapers;
}