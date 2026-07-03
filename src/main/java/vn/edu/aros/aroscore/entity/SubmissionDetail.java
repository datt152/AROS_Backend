package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "submission_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "selected_answer", length = 1)
    private String selectedAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    // Chi tiết này thuộc về bài nộp nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private ExamSubmission submission;

    // Trả lời cho câu hỏi nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}