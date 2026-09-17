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
            LEFT JOIN FETCH s.classroom
            WHERE s.id = :id AND e.teacher.email = :email
            """)
    Optional<ExamSession> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT s FROM ExamSession s
            JOIN FETCH s.exam e
            LEFT JOIN FETCH s.classroom
            WHERE e.id = :examId AND e.teacher.email = :email
            ORDER BY s.createdAt DESC
            """)
    List<ExamSession> findAllByExamIdAndTeacherEmail(@Param("examId") Long examId, @Param("email") String email);

    @Query("""
            SELECT s FROM ExamSession s
            JOIN FETCH s.exam e
            JOIN FETCH s.classroom c
            WHERE e.id = :examId AND c.id = :classroomId AND e.teacher.email = :email
            ORDER BY s.createdAt DESC
            """)
    List<ExamSession> findAllByExamIdAndClassroomIdAndTeacherEmail(
            @Param("examId") Long examId,
            @Param("classroomId") Long classroomId,
            @Param("email") String email);

    @Query("""
            SELECT COUNT(s) FROM ExamSession s
            WHERE s.exam.teacher.email = :email
              AND s.exam.examMode = vn.edu.aros.aroscore.entity.enums.ExamMode.OMR_PAPER
              AND s.status = :status
            """)
    long countOmrByTeacherEmailAndStatus(
            @Param("email") String email,
            @Param("status") vn.edu.aros.aroscore.entity.enums.ExamSessionStatus status);
}
