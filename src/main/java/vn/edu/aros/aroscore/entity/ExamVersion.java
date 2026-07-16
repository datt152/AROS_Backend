package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_versions")
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
    private Exam exam;

    @Column(name = "version_code", nullable = false)
    private String versionCode;

    @Column(name = "shuffle_matrix", columnDefinition = "TEXT", nullable = false)
    private String shuffleMatrix; // Chuỗi JSON lưu ma trận hoán vị
}