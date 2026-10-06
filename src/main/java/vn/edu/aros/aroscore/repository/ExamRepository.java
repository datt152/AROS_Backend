package vn.edu.aros.aroscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e WHERE e.teacher.email = :email")
    Page<Exam> findAllByTeacherEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.teacher.email = :email AND e.purpose = :purpose")
    Page<Exam> findAllByTeacherEmailAndPurpose(
            @Param("email") String email,
            @Param("purpose") ExamPurpose purpose,
            Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.id = :id AND e.teacher.email = :email")
    Optional<Exam> findByIdAndTeacherEmail(@Param("id") Long id, @Param("email") String email);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            LEFT JOIN FETCH e.examQuestions eq
            LEFT JOIN FETCH eq.question q
            WHERE e.id = :id
            """)
    Optional<Exam> findByIdWithQuestions(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            WHERE c.id = :classroomId AND e.teacher.email = :email
            """)
    Page<Exam> findAllByClassroomIdAndTeacherEmail(
            @Param("classroomId") Long classroomId,
            @Param("email") String email,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            WHERE c.id = :classroomId AND e.teacher.email = :email AND e.purpose = :purpose
            """)
    Page<Exam> findAllByClassroomIdAndTeacherEmailAndPurpose(
            @Param("classroomId") Long classroomId,
            @Param("email") String email,
            @Param("purpose") ExamPurpose purpose,
            Pageable pageable);

    /** Đề Online đã giao lớp SV; bỏ DRAFT. Không hiện đề OMR cho sinh viên. */
    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            JOIN c.students s
            LEFT JOIN e.onlineSettings os
            WHERE s.id = :studentId
              AND c.isActive = true
              AND (:purpose IS NULL OR e.purpose = :purpose)
              AND (:classroomId IS NULL OR c.id = :classroomId)
              AND e.examMode = vn.edu.aros.aroscore.entity.enums.ExamMode.ONLINE
              AND os IS NOT NULL
              AND os.status <> :draft
            """)
    Page<Exam> findAvailableForStudent(
            @Param("studentId") Long studentId,
            @Param("purpose") ExamPurpose purpose,
            @Param("classroomId") Long classroomId,
            @Param("draft") ExamStatus draft,
            Pageable pageable);

    /**
     * Đề Online (kỳ thi + luyện tập) đã giao lớp SV, bỏ DRAFT — dùng dashboard SV (không OMR).
     */
    @Query("""
            SELECT DISTINCT e FROM Exam e
            JOIN e.classrooms c
            JOIN c.students s
            JOIN FETCH e.subject
            JOIN FETCH e.onlineSettings os
            WHERE s.id = :studentId
              AND c.isActive = true
              AND e.examMode = vn.edu.aros.aroscore.entity.enums.ExamMode.ONLINE
              AND os.status <> :draft
            """)
    java.util.List<Exam> findOnlineAvailableForStudentDashboard(
            @Param("studentId") Long studentId,
            @Param("draft") ExamStatus draft);

    /** Lớp đã được giao ít nhất một đề OMR. */
    boolean existsByClassrooms_IdAndExamMode(Long classroomId, ExamMode examMode);

    @Query("""
            SELECT COUNT(e) FROM Exam e
            WHERE e.teacher.email = :email AND e.examMode = :mode AND e.purpose = :purpose
            """)
    long countByTeacherEmailAndModeAndPurpose(
            @Param("email") String email,
            @Param("mode") ExamMode mode,
            @Param("purpose") ExamPurpose purpose);

    @Query("""
            SELECT COUNT(e) FROM Exam e
            JOIN e.onlineSettings os
            WHERE e.teacher.email = :email
              AND e.examMode = :mode
              AND e.purpose = :purpose
              AND os.status = :status
            """)
    long countByTeacherEmailAndModePurposeAndOnlineStatus(
            @Param("email") String email,
            @Param("mode") ExamMode mode,
            @Param("purpose") ExamPurpose purpose,
            @Param("status") ExamStatus status);

    @Query("""
            SELECT DISTINCT e FROM Exam e
            LEFT JOIN FETCH e.subject
            LEFT JOIN FETCH e.onlineSettings
            LEFT JOIN FETCH e.paperSettings
            WHERE e.teacher.email = :email
            """)
    java.util.List<Exam> findAllByTeacherEmailWithSettings(@Param("email") String email);

    @Query("""
            SELECT e FROM Exam e
            WHERE e.teacher.email = :email
              AND e.purpose = vn.edu.aros.aroscore.entity.enums.ExamPurpose.EXAM
            """)
    java.util.List<Exam> findExamPurposeByTeacherEmail(@Param("email") String email);
}
