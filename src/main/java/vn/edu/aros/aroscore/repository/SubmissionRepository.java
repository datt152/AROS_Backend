package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.Submission;
import vn.edu.aros.aroscore.entity.User;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    boolean existsByExamAndStudent(Exam exam, User student);
}