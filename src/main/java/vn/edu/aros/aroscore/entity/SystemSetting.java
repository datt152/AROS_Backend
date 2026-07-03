package vn.edu.aros.aroscore.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_time_limit")
    private Integer examTimeLimit; // Map từ: thoi_gian_thi

    @Column(name = "max_questions")
    private Integer maxQuestions; // Map từ: so_cau_toi_da

    @Column(name = "max_upload_size")
    private Integer maxUploadSize; // Map từ: dung_luong_upload (Tính bằng MB)

    @Column(name = "max_login_attempts")
    private Integer maxLoginAttempts; // Map từ: so_lan_dang_nhap

    @Column(name = "session_timeout")
    private Integer sessionTimeout; // Map từ: thoi_gian_phien (Tính bằng phút)

    @Column(name = "force_password_change")
    private Boolean forcePasswordChange; // Map từ: bat_doi_mat_khau
}