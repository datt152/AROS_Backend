package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    // File này thuộc về kỳ thi nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // Một file có thể chứa nhiều kết quả (nếu là file scan nhiều bài)
    @OneToMany(mappedBy = "omrFile", cascade = CascadeType.ALL)
    private List<OMRResult> results;
}