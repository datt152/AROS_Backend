package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "omr_answer_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OmrAnswerDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "omr_file_id", nullable = false)
    private OMRFile omrFile;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "chosen", length = 10)
    private String chosen;

    @Column(name = "correct_answer", length = 10)
    private String correctAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "omr_status", length = 30)
    private String omrStatus;

    @Column(name = "bubble_json", columnDefinition = "TEXT")
    private String bubbleJson;
}
