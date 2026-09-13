package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.repository.AccountRepository;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.StudentCodeService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

/**
 * Ghi một dòng roster vào DB trong transaction riêng.
 */
@Service
@RequiredArgsConstructor
public class StudentRosterWriter {

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StudentCodeService studentCodeService;

    public enum Outcome {
        /** Tạo User mới và thêm vào lớp */
        CREATED,
        /** Cập nhật thông tin (tên/MSSV); có thể đã ở trong lớp hoặc vừa được thêm */
        UPDATED,
        /** User có sẵn, thêm vào lớp, không đổi hồ sơ */
        ENROLLED,
        /** Đã trong lớp và không có thay đổi hồ sơ */
        SKIPPED
    }

    @Transactional
    public Outcome upsertAndEnroll(Long classId, String email, String fullName, String codeFromFile) {
        Classroom classroom = classroomRepository.findByIdWithStudents(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học!"));

        Optional<Account> accountOpt = accountRepository.findByEmailIgnoreCase(email);
        if (accountOpt.isPresent() && accountOpt.get().getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Email này thuộc tài khoản không phải sinh viên");
        }

        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
        User user;
        boolean profileChanged;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.isActive()) {
                throw new IllegalArgumentException("Sinh viên không còn hoạt động");
            }
            String prevName = user.getFullName() != null ? user.getFullName().trim() : "";
            String prevCode = normalizeCode(user.getStudentCode());
            user.setFullName(fullName);
            applyStudentCode(user, codeFromFile, classId);
            userRepository.save(user);
            String newCode = normalizeCode(user.getStudentCode());
            profileChanged = !prevName.equals(fullName.trim()) || !Objects.equals(prevCode, newCode);
        } else {
            if (codeFromFile != null
                    && userRepository.existsStudentCodeInClassroom(classId, codeFromFile, null)) {
                throw new IllegalArgumentException(
                        "Mã sinh viên \"" + codeFromFile + "\" đã được dùng trong lớp này!");
            }
            user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .studentCode(codeFromFile)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
            profileChanged = true;
            enroll(classroom, user, classId);
            return Outcome.CREATED;
        }

        final Long userId = user.getId();
        if (classroom.getStudents() == null) {
            classroom.setStudents(new HashSet<>());
        }
        boolean alreadyIn = classroom.getStudents().stream()
                .anyMatch(s -> s.getId().equals(userId));

        if (alreadyIn) {
            return profileChanged ? Outcome.UPDATED : Outcome.SKIPPED;
        }

        enroll(classroom, user, classId);
        return profileChanged ? Outcome.UPDATED : Outcome.ENROLLED;
    }

    private void enroll(Classroom classroom, User user, Long classId) {
        if (classroom.getStudents() == null) {
            classroom.setStudents(new HashSet<>());
        }
        studentCodeService.assertNoStudentCodeConflictInClassroom(classId, user);
        classroom.getStudents().add(user);
        classroomRepository.save(classroom);
    }

    private void applyStudentCode(User user, String codeFromFile, Long classId) {
        if (codeFromFile == null) {
            return;
        }
        String current = user.getStudentCode();
        if (current != null && !current.isBlank()) {
            if (!current.trim().equals(codeFromFile)) {
                throw new IllegalArgumentException(
                        "Email đã có mã \"" + current.trim()
                                + "\", khác mã trong file \"" + codeFromFile + "\"");
            }
            return;
        }
        if (userRepository.existsStudentCodeInClassroom(classId, codeFromFile, user.getId())) {
            throw new IllegalArgumentException(
                    "Mã sinh viên \"" + codeFromFile + "\" đã được dùng trong lớp này!");
        }
        user.setStudentCode(codeFromFile);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }
}
