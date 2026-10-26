package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.ClassInsight;

import java.util.Optional;

public interface ClassInsightRepository extends JpaRepository<ClassInsight, Long> {

    Optional<ClassInsight> findFirstByExam_IdAndClassroom_IdAndExam_Teacher_EmailOrderByCreatedAtDesc(
            Long examId, Long classroomId, String email);
}
