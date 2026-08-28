package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e WHERE e.teacher.email = :email")
    Page<Exam> findAllByTeacherEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.teacher.email = :email AND e.purpose = :purpose")
    Page<Exam> findAllByTeacherEmailAndPurpose(
            @Param("email") String email,
            @Param("purpose") ExamPurpose purpose,
            Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.id = :id AND e.teacher.email = :email")
    Optional<Exam> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            LEFT JOIN FETCH e.examQuestions eq
            LEFT JOIN FETCH eq.question q
            LEFT JOIN FETCH q.options
            WHERE e.id = :id
            """)
    Optional<Exam> findByIdWithQuestions(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            WHERE c.id = :classroomId AND e.teacher.email = :email
            """)
    Page<Exam> findAllByClassroomIdAndTeacherEmail(
            @Param("classroomId") Long classroomId,
            @Param("email") String email,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            WHERE c.id = :classroomId AND e.teacher.email = :email AND e.purpose = :purpose
            """)
    Page<Exam> findAllByClassroomIdAndTeacherEmailAndPurpose(
            @Param("classroomId") Long classroomId,
            @Param("email") String email,
            @Param("purpose") ExamPurpose purpose,
            Pageable pageable);

    /** Đề đã giao cho lớp mà student đang học; bỏ DRAFT. */
    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            JOIN c.students s
            WHERE s.id = :studentId
              AND c.isActive = true
              AND e.status <> :draft
              AND (:purpose IS NULL OR e.purpose = :purpose)
              AND (:classroomId IS NULL OR c.id = :classroomId)
            """)
    Page<Exam> findAvailableForStudent(
            @Param("studentId") Long studentId,
            @Param("purpose") ExamPurpose purpose,
            @Param("classroomId") Long classroomId,
            @Param("draft") ExamStatus draft,
            Pageable pageable);
}
