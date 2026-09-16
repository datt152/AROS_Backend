package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.OMRFile;
import vn.edu.aros.aroscore.entity.enums.OmrSheetStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OMRFileRepository extends JpaRepository<OMRFile, Long> {

    List<OMRFile> findByExamId(Long examId);

    List<OMRFile> findByExamSessionIdOrderByUploadTimeDesc(Long examSessionId);

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
            FROM OMRFile f
            WHERE f.examSession.id = :sessionId
              AND f.contentHash = :contentHash
              AND f.status IN :statuses
            """)
    boolean existsDuplicateInSession(
            @Param("sessionId") Long sessionId,
            @Param("contentHash") String contentHash,
            @Param("statuses") Collection<OmrSheetStatus> statuses);

    @Query("""
            SELECT f FROM OMRFile f
            LEFT JOIN FETCH f.answerDetails
            LEFT JOIN FETCH f.student
            JOIN FETCH f.exam e
            JOIN FETCH e.teacher
            LEFT JOIN FETCH f.examSession es
            LEFT JOIN FETCH es.classroom
            WHERE f.id = :id
            """)
    Optional<OMRFile> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT f FROM OMRFile f
            JOIN FETCH f.exam e
            JOIN FETCH e.teacher
            WHERE f.id = :id AND e.teacher.email = :email
            """)
    Optional<OMRFile> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);
}
