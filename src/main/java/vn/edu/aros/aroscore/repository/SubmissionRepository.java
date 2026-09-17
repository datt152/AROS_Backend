package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.Submission;
import vn.edu.aros.aroscore.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByExamAndStudentAndClassroomAndSubmitTimeIsNull(
            Exam exam, User student, Classroom classroom);

    boolean existsByExamAndStudentAndClassroomAndSubmitTimeIsNotNull(
            Exam exam, User student, Classroom classroom);

    long countByExamAndStudentAndClassroomAndSubmitTimeIsNotNull(
            Exam exam, User student, Classroom classroom);

    boolean existsByExamAndSubmitTimeIsNotNull(Exam exam);

    long countByExamAndSubmitTimeIsNotNull(Exam exam);

    List<Submission> findByExam(Exam exam);

    @Query("""
            SELECT COALESCE(MAX(s.attemptNo), 0) FROM Submission s
            WHERE s.exam = :exam AND s.student = :student AND s.classroom = :classroom
            """)
    int findMaxAttemptNo(
            @Param("exam") Exam exam,
            @Param("student") User student,
            @Param("classroom") Classroom classroom);

    @Query("SELECT s FROM Submission s JOIN FETCH s.student WHERE s.exam.id = :examId")
    List<Submission> findAllByExamIdWithStudent(@Param("examId") Long examId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student
            WHERE s.exam.id = :examId AND s.classroom.id = :classroomId
            """)
    List<Submission> findAllByExamIdAndClassroomIdWithStudent(
            @Param("examId") Long examId,
            @Param("classroomId") Long classroomId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student
            JOIN FETCH s.exam e
            JOIN FETCH e.teacher
            LEFT JOIN FETCH s.classroom
            WHERE s.id = :id
            """)
    Optional<Submission> findByIdWithStudentAndExam(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT s FROM Submission s
            JOIN FETCH s.student
            LEFT JOIN FETCH s.details d
            LEFT JOIN FETCH d.question
            WHERE s.exam.id = :examId AND s.submitTime IS NOT NULL
            """)
    List<Submission> findSubmittedWithDetailsByExamId(@Param("examId") Long examId);

    @Query("""
            SELECT DISTINCT s FROM Submission s
            JOIN FETCH s.student
            LEFT JOIN FETCH s.details d
            LEFT JOIN FETCH d.question
            WHERE s.exam.id = :examId
              AND s.classroom.id = :classroomId
              AND s.submitTime IS NOT NULL
            """)
    List<Submission> findSubmittedWithDetailsByExamIdAndClassroomId(
            @Param("examId") Long examId,
            @Param("classroomId") Long classroomId);

    void deleteByExam(Exam exam);

    @Query("""
            SELECT s FROM Submission s
            LEFT JOIN FETCH s.classroom
            WHERE s.student.id = :studentId AND s.exam.id IN :examIds
            """)
    List<Submission> findAllByStudentIdAndExamIdIn(
            @Param("studentId") Long studentId,
            @Param("examIds") java.util.Collection<Long> examIds);

    @Query("""
            SELECT s FROM Submission s
            LEFT JOIN FETCH s.classroom
            WHERE s.student.id = :studentId
              AND s.exam.id = :examId
              AND s.classroom.id = :classroomId
            """)
    List<Submission> findAllByStudentIdAndExamIdAndClassroomId(
            @Param("studentId") Long studentId,
            @Param("examId") Long examId,
            @Param("classroomId") Long classroomId);

    @Query("""
            SELECT DISTINCT s FROM Submission s
            JOIN FETCH s.student
            JOIN FETCH s.exam e
            JOIN FETCH e.teacher
            LEFT JOIN FETCH s.classroom
            LEFT JOIN FETCH s.details d
            LEFT JOIN FETCH d.question
            WHERE s.id = :id
            """)
    Optional<Submission> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.exam e
            LEFT JOIN FETCH s.classroom
            WHERE s.student.id = :studentId
              AND (:examId IS NULL OR e.id = :examId)
            """)
    List<Submission> findAllByStudentIdAndOptionalExamId(
            @Param("studentId") Long studentId,
            @Param("examId") Long examId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.exam e
            LEFT JOIN FETCH e.subject
            LEFT JOIN FETCH s.classroom
            WHERE s.student.id = :studentId
              AND s.submitTime IS NOT NULL
              AND s.submitTime >= :since
            ORDER BY s.submitTime DESC
            """)
    List<Submission> findSubmittedByStudentSince(
            @Param("studentId") Long studentId,
            @Param("since") java.time.LocalDateTime since);

    @Query("""
            SELECT COUNT(s) FROM Submission s
            WHERE s.student.id = :studentId
              AND s.submitTime IS NOT NULL
              AND s.submitTime >= :since
            """)
    long countSubmittedByStudentSince(
            @Param("studentId") Long studentId,
            @Param("since") java.time.LocalDateTime since);
}
