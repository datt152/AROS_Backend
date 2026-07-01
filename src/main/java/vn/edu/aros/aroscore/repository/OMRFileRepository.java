package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.OMRFile;

import java.util.List;

public interface OMRFileRepository extends JpaRepository<OMRFile, Long> {
    List<OMRFile> findByExamId(Long examId);
}