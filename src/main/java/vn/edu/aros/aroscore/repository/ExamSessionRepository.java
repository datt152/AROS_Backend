package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.ExamSession;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    @Query("""
            SELECT s FROM ExamSession s
            JOIN FETCH s.exam e
            JOIN FETCH e.teacher
            WHERE s.id = :id AND e.teacher.email = :email
            """)
    Optional<ExamSession> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT s FROM ExamSession s
            JOIN FETCH s.exam e
            WHERE e.id = :examId AND e.teacher.email = :email
            ORDER BY s.createdAt DESC
            """)
    List<ExamSession> findAllByExamIdAndTeacherEmail(@Param("examId") Long examId, @Param("email") String email);
}
