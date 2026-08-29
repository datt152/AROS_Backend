package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Topic;

import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Query("""
            SELECT t FROM Topic t
            WHERE t.subject.id = :subjectId
              AND t.subject.lecturer.email = :email
              AND (:includeInactive = true OR t.isActive = true)
            """)
    Page<Topic> findAllBySubjectIdAndLecturerEmail(
            @Param("subjectId") Long subjectId,
            @Param("email") String email,
            @Param("includeInactive") boolean includeInactive,
            Pageable pageable);

    @Query("""
            SELECT t FROM Topic t
            WHERE t.id = :id AND t.subject.lecturer.email = :email
            """)
    Optional<Topic> findByIdAndLecturerEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT t FROM Topic t
            WHERE t.id = :id AND t.subject.lecturer.email = :email AND t.isActive = true
            """)
    Optional<Topic> findActiveByIdAndLecturerEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
            FROM Topic t
            WHERE t.name = :name AND t.subject.id = :subjectId AND t.isActive = true
            """)
    boolean existsByNameAndSubjectId(@Param("name") String name, @Param("subjectId") Long subjectId);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
            FROM Topic t
            WHERE t.name = :name AND t.subject.id = :subjectId AND t.id <> :excludeId AND t.isActive = true
            """)
    boolean existsByNameAndSubjectIdAndIdNot(
            @Param("name") String name,
            @Param("subjectId") Long subjectId,
            @Param("excludeId") Long excludeId);
}
