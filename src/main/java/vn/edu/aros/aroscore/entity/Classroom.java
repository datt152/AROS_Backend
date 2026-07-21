package vn.edu.aros.aroscore.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "classrooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String semester;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Lớp này thuộc về Môn học nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // Quan hệ N-N: Một lớp có nhiều sinh viên, sinh viên học nhiều lớp
    // JPA sẽ tự động tạo bảng trung gian 'classroom_students'
    @ManyToMany
    @JsonIgnore
    @JoinTable(
            name = "classroom_students",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<User> students;
}