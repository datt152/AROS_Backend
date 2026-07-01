package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySubjectId(Long subjectId);
    List<Exam> findByStatus(ExamStatus status);
}