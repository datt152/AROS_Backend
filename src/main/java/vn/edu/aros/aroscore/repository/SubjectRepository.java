package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    // 1. Chỉ lấy danh sách môn học của chính giáo viên đang request
    @Query("SELECT s FROM Subject s WHERE s.lecturer.email = :email")
    Page<Subject> findAllByLecturerEmail(@Param("email") String email, Pageable pageable);

    // 2. Tìm môn học theo ID nhưng phải đảm bảo thuộc về đúng giáo viên đó
    @Query("SELECT s FROM Subject s WHERE s.id = :id AND s.lecturer.email = :email")
    Optional<Subject> findByIdAndLecturerEmail(@Param("id") Long id, @Param("email") String email);

    // 3. Kiểm tra trùng tên môn học nhưng CHỈ trong phạm vi các môn của giáo viên đó
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM Subject s WHERE s.subjectName = :subjectName AND s.lecturer.email = :email")
    boolean existsBySubjectNameAndLecturerEmail(@Param("subjectName") String subjectName, @Param("email") String email);
}
