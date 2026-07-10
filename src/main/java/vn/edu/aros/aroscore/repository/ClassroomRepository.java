package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.Classroom;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    @Query("SELECT c FROM Classroom c WHERE c.subject.lecturer.email = :email")
    Page<Classroom> findAllByLecturerEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT c FROM Classroom c WHERE c.subject.id = :subjectId AND c.subject.lecturer.email = :email")
    Page<Classroom> findAllBySubjectIdAndLecturerEmail(@Param("subjectId") Long subjectId, @Param("email") String email, Pageable pageable);

    boolean existsByClassNameAndSubjectId(String className, Long subjectId);
}

