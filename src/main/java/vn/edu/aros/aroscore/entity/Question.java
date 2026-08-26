package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.aros.aroscore.entity.enums.Difficulty;
import vn.edu.aros.aroscore.entity.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /** Chủ đề / chương trong môn (optional để tương thích câu hỏi cũ). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    // Quan hệ 1-N: 1 Câu hỏi có nhiều Đáp án
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerOption> options = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private QuestionType type = QuestionType.SINGLE_CHOICE;

    /** Soft delete: false = ẩn khỏi ngân hàng, vẫn giữ cho exam đã gắn. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Hàm tiện ích để đồng bộ quan hệ 2 chiều
    public void addOption(AnswerOption option) {
        options.add(option);
        option.setQuestion(this);
    }
}