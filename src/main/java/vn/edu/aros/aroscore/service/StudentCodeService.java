package vn.edu.aros.aroscore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.utils.StudentCodeRules;

@Service
@RequiredArgsConstructor
public class StudentCodeService {

    private final UserRepository userRepository;

    /**
     * Validate format 8 số và đảm bảo không trùng mã với SV khác trong lớp.
     * @return mã đã chuẩn hóa (trim)
     */
    public String validateForClassroom(String studentCode, Long classroomId, Long excludeUserId) {
        String normalized = StudentCodeRules.requireValidOmrFormat(studentCode);

        boolean duplicated = userRepository.existsStudentCodeInClassroom(
                classroomId, normalized, excludeUserId);
        if (duplicated) {
            throw new RuntimeException(
                    "Mã sinh viên \"" + normalized + "\" đã được dùng bởi sinh viên khác trong lớp này!");
        }
        return normalized;
    }

    /**
     * Chỉ kiểm tra format
     */
    public String validateFormat(String studentCode) {
        return StudentCodeRules.requireValidOmrFormat(studentCode);
    }
}
