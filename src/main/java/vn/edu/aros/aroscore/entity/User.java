package vn.edu.aros.aroscore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "student_code", unique = true, length = 20)
    private String studentCode; // mssv

    @Column(name = "is_active")
    private boolean isActive = true;

    // Quan hệ 1-1 với Account. mappedBy chỉ ra Account là bên giữ khóa ngoại
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Account account;
}
