package vn.edu.aros.aroscore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_versions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_version_code", columnNames = {"exam_id", "version_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    @JsonIgnore
    private Exam exam;

    @Column(name = "version_code", nullable = false)
    private String versionCode;

    @Column(name = "shuffle_matrix", columnDefinition = "TEXT", nullable = false)
    private String shuffleMatrix;
}
