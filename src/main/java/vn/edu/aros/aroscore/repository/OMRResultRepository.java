package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.OMRResult;

import java.util.List;

public interface OMRResultRepository extends JpaRepository<OMRResult, Long> {
    List<OMRResult> findByOmrFileId(Long omrFileId);
}