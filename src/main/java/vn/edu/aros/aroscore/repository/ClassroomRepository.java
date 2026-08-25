package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.Classroom;

import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    @Query("SELECT c FROM Classroom c WHERE c.subject.lecturer.email = :email")
    Page<Classroom> findAllByLecturerEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT c FROM Classroom c WHERE c.subject.id = :subjectId AND c.subject.lecturer.email = :email")
    Page<Classroom> findAllBySubjectIdAndLecturerEmail(@Param("subjectId") Long subjectId, @Param("email") String email, Pageable pageable);

    boolean existsByClassNameAndSubjectId(String className, Long subjectId);

    @Query("SELECT DISTINCT c FROM Classroom c LEFT JOIN FETCH c.students s LEFT JOIN FETCH s.account WHERE c.id = :id")
    Optional<Classroom> findByIdWithStudents(@Param("id") Long id);

    @Query("SELECT c FROM Classroom c WHERE c.id IN :ids AND c.subject.id = :subjectId AND c.subject.lecturer.email = :email")
    java.util.List<Classroom> findAllByIdsAndSubjectAndLecturer(
            @Param("ids") java.util.Collection<Long> ids,
            @Param("subjectId") Long subjectId,
            @Param("email") String email);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Classroom c JOIN c.students s
            WHERE c.id IN :classroomIds AND s.id = :studentId AND c.isActive = true
            """)
    boolean isStudentInAnyClassroom(
            @Param("classroomIds") java.util.Collection<Long> classroomIds,
            @Param("studentId") Long studentId);
}

