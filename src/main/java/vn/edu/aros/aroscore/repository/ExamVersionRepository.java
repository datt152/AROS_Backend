package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.ExamVersion;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamVersionRepository extends JpaRepository<ExamVersion, Long> {

    Optional<ExamVersion> findByExamIdAndVersionCode(Long examId, String versionCode);

    List<ExamVersion> findByExamId(Long examId);

    boolean existsByExamIdAndVersionCode(Long examId, String versionCode);

    void deleteByExamIdAndVersionCodeIn(Long examId, List<String> versionCodes);
}
