package vn.edu.aros.aroscore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.utils.StudentCodeRules;

import java.util.List;

/**
 * Validate format MSSV (8 số) và không trùng trong cùng một lớp.
 */
@Service
@RequiredArgsConstructor
public class StudentCodeService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;

    /** Lớp đã được giao ≥1 đề OMR → thêm SV bắt buộc có MSSV hợp lệ. */
    public boolean classroomRequiresStudentCode(Long classroomId) {
        return examRepository.existsByClassrooms_IdAndExamMode(classroomId, ExamMode.OMR_PAPER);
    }

    /**
     * Khi thêm SV vào lớp đã có đề OMR: bắt buộc MSSV 8 số.
     */
    public void assertStudentCodeRequiredWhenOmrAssigned(Long classroomId, User student) {
        if (!classroomRequiresStudentCode(classroomId)) {
            return;
        }
        String code = student.getStudentCode();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Lớp đã có đề thi OMR - không thể thêm sinh viên " + student.getEmail());
        }
        StudentCodeRules.requireValidOmrFormat(code);
    }

    /**
     * Khi thêm SV vào lớp: nếu SV đã có MSSV thì không được trùng với SV khác trong lớp.
     */
    public void assertNoStudentCodeConflictInClassroom(Long classroomId, User student) {
        String code = student.getStudentCode();
        if (code == null || code.isBlank()) {
            return;
        }
        String normalized = code.trim();
        boolean duplicated = userRepository.existsStudentCodeInClassroom(
                classroomId, normalized, student.getId());
        if (duplicated) {
            throw new IllegalArgumentException(
                    "Không thể thêm \"" + student.getEmail() + "\": mã sinh viên \""
                            + normalized + "\" đã tồn tại!");
        }
    }

    /**
     * Kiểm tra trùng MSSV giữa các SV sắp enroll cùng lúc (chưa có trong lớp).
     */
    public void assertNoStudentCodeConflictInBatch(List<User> students) {
        java.util.Map<String, String> codeToEmail = new java.util.HashMap<>();
        for (User student : students) {
            String code = student.getStudentCode();
            if (code == null || code.isBlank()) {
                continue;
            }
            String normalized = code.trim();
            String existingEmail = codeToEmail.putIfAbsent(normalized, student.getEmail());
            if (existingEmail != null) {
                throw new IllegalArgumentException(
                        "Trùng mã sinh viên \"" + normalized + "\" giữa "
                                + existingEmail + " và " + student.getEmail()
                                + " trong danh sách thêm vào lớp!");
            }
        }
    }

    /**
     * Validate format + không trùng trong một lớp cụ thể.
     */
    public String validateForClassroom(String studentCode, Long classroomId, Long excludeUserId) {
        String normalized = StudentCodeRules.requireValidOmrFormat(studentCode);

        boolean duplicated = userRepository.existsStudentCodeInClassroom(
                classroomId, normalized, excludeUserId);
        if (duplicated) {
            throw new IllegalArgumentException(
                    "Mã sinh viên \"" + normalized + "\" đã tồn tại!");
        }
        return normalized;
    }

    /**
     * Validate format + không trùng trong mọi lớp mà user đang thuộc.
     * Dùng khi SV tự cập nhật mã (có thể học nhiều lớp).
     */
    public String validateForAllClassroomsOfUser(String studentCode, Long userId) {
        String normalized = StudentCodeRules.requireValidOmrFormat(studentCode);

        List<Long> classroomIds = userRepository.findActiveClassroomIdsByStudentId(userId);
        for (Long classroomId : classroomIds) {
            boolean duplicated = userRepository.existsStudentCodeInClassroom(
                    classroomId, normalized, userId);
            if (duplicated) {
                throw new IllegalArgumentException(
                        "Mã sinh viên \"" + normalized
                                + "\"  đã tồn tại!");
            }
        }
        return normalized;
    }

    public String validateFormat(String studentCode) {
        return StudentCodeRules.requireValidOmrFormat(studentCode);
    }
}
