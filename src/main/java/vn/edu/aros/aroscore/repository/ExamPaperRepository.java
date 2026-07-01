package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.ExamPaper;

import java.util.List;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    List<ExamPaper> findByExamId(Long examId);
}