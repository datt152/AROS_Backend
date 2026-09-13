package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.response.StudentImportResultResponse;
import vn.edu.aros.aroscore.dto.response.StudentImportResultResponse.StudentImportRowResult;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.service.StudentImportService;
import vn.edu.aros.aroscore.service.excel.StudentExcelParser;
import vn.edu.aros.aroscore.service.excel.StudentExcelParser.ParsedRow;
import vn.edu.aros.aroscore.utils.StudentCodeRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentImportServiceImpl implements StudentImportService {

    private final ClassroomRepository classroomRepository;
    private final StudentExcelParser studentExcelParser;
    private final StudentRosterWriter studentRosterWriter;

    @Override
    public StudentImportResultResponse importStudentsFromExcel(Long classId, MultipartFile file) {
        getAuthorizedActiveClassroom(classId);
        String teacherEmail = getCurrentUserEmail();

        List<ParsedRow> rows = studentExcelParser.parse(file);

        int success = 0;
        int failed = 0;
        int skipped = 0;
        List<StudentImportRowResult> errors = new ArrayList<>();
        List<StudentImportRowResult> successes = new ArrayList<>();
        Set<String> emailsInFile = new HashSet<>();
        Map<String, Integer> codesInFile = new HashMap<>();

        for (ParsedRow row : rows) {
            try {
                String email = normalizeEmail(row.email());
                if (email == null || email.isBlank()) {
                    throw new IllegalArgumentException("Email không được để trống");
                }
                if (!email.contains("@")) {
                    throw new IllegalArgumentException("Email không hợp lệ");
                }
                if (email.equalsIgnoreCase(teacherEmail)) {
                    throw new IllegalArgumentException("Không thể thêm email của chính giáo viên");
                }
                if (!emailsInFile.add(email)) {
                    throw new IllegalArgumentException("Email bị trùng trong file");
                }

                String fullName = row.fullName() != null ? row.fullName().trim() : "";
                if (fullName.isBlank()) {
                    throw new IllegalArgumentException("Họ tên không được để trống");
                }

                String codeFromFile = null;
                if (row.studentCode() != null && !row.studentCode().isBlank()) {
                    codeFromFile = StudentCodeRules.requireValidOmrFormat(row.studentCode());
                    Integer otherRow = codesInFile.putIfAbsent(codeFromFile, row.rowNumber());
                    if (otherRow != null) {
                        throw new IllegalArgumentException(
                                "Mã sinh viên \"" + codeFromFile + "\" trùng với dòng " + otherRow + " trong file");
                    }
                }

                StudentRosterWriter.Outcome outcome =
                        studentRosterWriter.upsertAndEnroll(classId, email, fullName, codeFromFile);

                String msg = switch (outcome) {
                    case CREATED -> "Tạo hồ sơ và thêm vào lớp thành công";
                    case UPDATED -> "Cập nhật thông tin sinh viên thành công";
                    case ENROLLED -> "Thêm sinh viên vào lớp thành công";
                    case SKIPPED -> "Đã có trong lớp";
                };

                if (outcome == StudentRosterWriter.Outcome.SKIPPED) {
                    skipped++;
                } else {
                    success++;
                }
                successes.add(rowResult(row.rowNumber(), email, fullName, codeFromFile, msg));
            } catch (Exception ex) {
                failed++;
                errors.add(StudentImportRowResult.builder()
                        .row(row.rowNumber())
                        .email(row.email())
                        .fullName(row.fullName())
                        .studentCode(row.studentCode())
                        .message(ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định")
                        .build());
            }
        }

        return StudentImportResultResponse.builder()
                .total(rows.size())
                .success(success)
                .failed(failed)
                .skipped(skipped)
                .errors(errors)
                .successes(successes)
                .build();
    }

    private StudentImportRowResult rowResult(
            int row, String email, String fullName, String code, String message) {
        return StudentImportRowResult.builder()
                .row(row)
                .email(email)
                .fullName(fullName)
                .studentCode(code)
                .message(message)
                .build();
    }

    private Classroom getAuthorizedActiveClassroom(Long classId) {
        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));
        String email = getCurrentUserEmail();
        if (!classroom.getSubject().getLecturer().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền truy cập lớp học này!");
        }
        if (Boolean.FALSE.equals(classroom.getIsActive())) {
            throw new IllegalArgumentException("Lớp học đã ngừng hoạt động, không thể import!");
        }
        return classroom;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
