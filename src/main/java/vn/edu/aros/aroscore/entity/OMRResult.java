package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "omr_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OMRResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double score;

    // Kết quả này trích xuất từ file nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private OMRFile omrFile;

    // Điểm này là của sinh viên nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
}
