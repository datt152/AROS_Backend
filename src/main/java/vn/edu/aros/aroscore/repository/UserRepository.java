package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.aros.aroscore.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Deprecated
    Optional<User> findByStudentCode(String studentCode);

    boolean existsByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);

    @Query("SELECT u FROM User u JOIN FETCH u.account WHERE u.email IN :emails")
    List<User> findAllByEmailInWithAccount(@Param("emails") List<String> emails);

    @Query("SELECT u FROM User u JOIN FETCH u.account WHERE u.id = :id")
    Optional<User> findByIdWithAccount(@Param("id") Long id);

    /**
     * Trùng mã sinh viên trong cùng một lớp
     */
    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM Classroom c JOIN c.students s
            WHERE c.id = :classroomId
              AND s.studentCode = :studentCode
              AND (:excludeUserId IS NULL OR s.id <> :excludeUserId)
            """)
    boolean existsStudentCodeInClassroom(
            @Param("classroomId") Long classroomId,
            @Param("studentCode") String studentCode,
            @Param("excludeUserId") Long excludeUserId);

    /**
     * Tìm SV theo mã trong phạm vi các lớp (dùng cho match OMR theo đề/lớp).
     */
    @Query("""
            SELECT DISTINCT s FROM Classroom c JOIN c.students s
            WHERE c.id IN :classroomIds AND s.studentCode = :studentCode
            """)
    List<User> findByStudentCodeInClassrooms(
            @Param("classroomIds") java.util.Collection<Long> classroomIds,
            @Param("studentCode") String studentCode);

    @Query("""
            SELECT c.id FROM Classroom c JOIN c.students s
            WHERE s.id = :studentId AND c.isActive = true
            """)
    List<Long> findActiveClassroomIdsByStudentId(@Param("studentId") Long studentId);
}