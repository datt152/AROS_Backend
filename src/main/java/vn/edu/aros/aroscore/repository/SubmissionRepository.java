package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.Submission;
import vn.edu.aros.aroscore.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByExamAndStudentAndSubmitTimeIsNull(Exam exam, User student);

    boolean existsByExamAndStudentAndSubmitTimeIsNotNull(Exam exam, User student);

    long countByExamAndStudentAndSubmitTimeIsNotNull(Exam exam, User student);

    boolean existsByExamAndSubmitTimeIsNotNull(Exam exam);

    long countByExamAndSubmitTimeIsNotNull(Exam exam);

    List<Submission> findByExam(Exam exam);

    @Query("SELECT COALESCE(MAX(s.attemptNo), 0) FROM Submission s WHERE s.exam = :exam AND s.student = :student")
    int findMaxAttemptNo(@Param("exam") Exam exam, @Param("student") User student);

    @Query("SELECT s FROM Submission s JOIN FETCH s.student WHERE s.exam.id = :examId")
    List<Submission> findAllByExamIdWithStudent(@Param("examId") Long examId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.student
            JOIN FETCH s.exam e
            JOIN FETCH e.teacher
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

    void deleteByExam(Exam exam);
}
