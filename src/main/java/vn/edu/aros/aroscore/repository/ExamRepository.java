package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;

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
}
