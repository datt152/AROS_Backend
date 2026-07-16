package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.ExamVersion;
import java.util.Optional;

@Repository
public interface ExamVersionRepository extends JpaRepository<ExamVersion, Long> {
    // Dùng để OMR tìm kiếm mã đề khi chấm điểm
    Optional<ExamVersion> findByExamIdAndVersionCode(Long examId, String versionCode);
}