package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    // Lấy danh sách đề thi của một giáo viên
    Page<Exam> findAllByTeacherEmail(String email, Pageable pageable);
}