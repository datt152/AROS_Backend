package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.ExamTemplate;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;

import java.util.Optional;

@Repository
public interface ExamTemplateRepository extends JpaRepository<ExamTemplate, Long> {

    @Query("""
            SELECT t FROM ExamTemplate t
            WHERE t.teacher.email = :email AND t.isActive = true
            """)
    Page<ExamTemplate> findAllActiveByTeacherEmail(@Param("email") String email, Pageable pageable);

    @Query("""
            SELECT t FROM ExamTemplate t
            WHERE t.teacher.email = :email AND t.isActive = true AND t.purpose = :purpose
            """)
    Page<ExamTemplate> findAllActiveByTeacherEmailAndPurpose(
            @Param("email") String email,
            @Param("purpose") ExamPurpose purpose,
            Pageable pageable);

    @Query("""
            SELECT t FROM ExamTemplate t
            WHERE t.teacher.email = :email AND t.isActive = true AND t.subject.id = :subjectId
            """)
    Page<ExamTemplate> findAllActiveByTeacherEmailAndSubjectId(
            @Param("email") String email,
            @Param("subjectId") Long subjectId,
            Pageable pageable);

    @Query("""
            SELECT t FROM ExamTemplate t
            WHERE t.teacher.email = :email AND t.isActive = true
              AND t.subject.id = :subjectId AND t.purpose = :purpose
            """)
    Page<ExamTemplate> findAllActiveByTeacherEmailAndSubjectIdAndPurpose(
            @Param("email") String email,
            @Param("subjectId") Long subjectId,
            @Param("purpose") ExamPurpose purpose,
            Pageable pageable);

    @Query("""
            SELECT t FROM ExamTemplate t
            WHERE t.id = :id AND t.teacher.email = :email AND t.isActive = true
            """)
    Optional<ExamTemplate> findActiveByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT t FROM ExamTemplate t
            WHERE t.id = :id AND t.teacher.email = :email
            """)
    Optional<ExamTemplate> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);
}
