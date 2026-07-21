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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_answer")
    private String selectedAnswer; // Đáp án học sinh tô (A, B, C, D)

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;
}