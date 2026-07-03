package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_name", nullable = false, length = 150)
    private String subjectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Giảng viên phụ trách môn học
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private User lecturer;

    // Một môn học có thể mở nhiều lớp
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    private List<Classroom> classrooms;

    // Một môn học có nhiều câu hỏi trong ngân hàng
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    private List<Question> questions;
}