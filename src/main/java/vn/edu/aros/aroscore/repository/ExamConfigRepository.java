package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.ExamConfig;

import java.util.Optional;

public interface ExamConfigRepository extends JpaRepository<ExamConfig, Long> {
    Optional<ExamConfig> findByExamId(Long examId);
}
