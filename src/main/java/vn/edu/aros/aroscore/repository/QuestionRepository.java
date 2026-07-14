package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Question;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE q.teacher.email = :email")
    Page<Question> findAllByTeacherEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.subject.id = :subjectId AND q.teacher.email = :email")
    Page<Question> findAllBySubjectIdAndTeacherEmail(@Param("subjectId") Long subjectId, @Param("email") String email, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.id = :id AND q.teacher.email = :email")
    Optional<Question> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);
}