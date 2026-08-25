package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.Submission;
import vn.edu.aros.aroscore.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByExamAndStudent(Exam exam, User student);

    boolean existsByExamAndStudentAndSubmitTimeIsNotNull(Exam exam, User student);

    boolean existsByExamAndSubmitTimeIsNotNull(Exam exam);

    long countByExamAndSubmitTimeIsNotNull(Exam exam);

    List<Submission> findByExam(Exam exam);

    void deleteByExam(Exam exam);
}
