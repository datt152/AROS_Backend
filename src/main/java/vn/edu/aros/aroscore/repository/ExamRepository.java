package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e WHERE e.teacher.email = :email")
    Page<Exam> findAllByTeacherEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.id = :id AND e.teacher.email = :email")
    Optional<Exam> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);
}
