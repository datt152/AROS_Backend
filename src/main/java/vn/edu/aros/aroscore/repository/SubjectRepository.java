package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.Subject;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @Query("""
            SELECT s FROM Subject s
            WHERE s.lecturer.email = :email
              AND (:includeInactive = true OR s.isActive = true)
            """)
    Page<Subject> findAllByLecturerEmail(
            @Param("email") String email,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE s.id = :id AND s.lecturer.email = :email")
    Optional<Subject> findByIdAndLecturerEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT s FROM Subject s
            WHERE s.id = :id AND s.lecturer.email = :email AND s.isActive = true
            """)
    Optional<Subject> findActiveByIdAndLecturerEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM Subject s
            WHERE s.subjectName = :subjectName
              AND s.lecturer.email = :email
              AND s.isActive = true
            """)
    boolean existsBySubjectNameAndLecturerEmail(
            @Param("subjectName") String subjectName,
            @Param("email") String email);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM Subject s
            WHERE s.subjectName = :subjectName
              AND s.lecturer.email = :email
              AND s.id <> :excludeId
              AND s.isActive = true
            """)
    boolean existsBySubjectNameAndLecturerEmailAndIdNot(
            @Param("subjectName") String subjectName,
            @Param("email") String email,
            @Param("excludeId") Long excludeId);
}
