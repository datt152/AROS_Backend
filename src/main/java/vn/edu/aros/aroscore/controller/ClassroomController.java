package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.request.BulkCreateAccountsRequest;
import vn.edu.aros.aroscore.dto.request.ClassroomRequest;
import vn.edu.aros.aroscore.dto.request.ClassroomUpdateRequest;
import vn.edu.aros.aroscore.dto.request.EnrollStudentRequest;
import vn.edu.aros.aroscore.dto.request.UpdateStudentCodeRequest;
import vn.edu.aros.aroscore.dto.response.BulkCreateAccountsResultResponse;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.dto.response.StudentImportResultResponse;
import vn.edu.aros.aroscore.dto.response.StudentInfoResponse;
import vn.edu.aros.aroscore.service.BulkAccountService;
import vn.edu.aros.aroscore.service.ClassroomService;
import vn.edu.aros.aroscore.service.StudentImportService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private StudentImportService studentImportService;

    @Autowired
    private BulkAccountService bulkAccountService;

    @PostMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> createClassroom(@Valid @RequestBody ClassroomRequest request) {
        return new ResponseEntity<>(classroomService.createClassroom(request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
//    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ClassroomResponse>> getMyClassrooms(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(classroomService.getMyClassrooms(subjectId, page, size));
    }

    @GetMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<ClassroomResponse>> getAllClassrooms(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        return ResponseEntity.ok(classroomService.getAllClassrooms(subjectId, page, size, includeInactive));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> getClassroomById(@PathVariable Long id) {
        return ResponseEntity.ok(classroomService.getClassroomById(id));
    }

    @GetMapping("/{id}/students")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentInfoResponse>> getClassroomStudents(@PathVariable Long id) {
        return ResponseEntity.ok(classroomService.getClassroomStudents(id));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> updateClassroom(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomUpdateRequest request) {
        return ResponseEntity.ok(classroomService.updateClassroom(id, request));
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteClassroom(@PathVariable Long id) {
        classroomService.deleteClassroom(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/students/enroll")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> enrollStudents(
            @PathVariable Long id,
            @Valid @RequestBody EnrollStudentRequest request) {

        classroomService.enrollStudents(id, request);
        return ResponseEntity.ok("Thêm sinh viên vào lớp thành công!");
    }

    /**
     * Import SV từ Excel (.xlsx) — template:
     * A=STT (bỏ qua) | B=Họ đệm | C=Tên | D=MSSV | E=Email.
     * Dòng 1 = tiêu đề. Tạo User nếu chưa có (chưa cần Account).
     */
    @PostMapping(value = "/{id}/students/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentImportResultResponse> importStudents(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(studentImportService.importStudentsFromExcel(id, file));
    }

    /**
     * Tạo tài khoản đăng nhập hàng loạt cho SV trong lớp (chưa có Account).
     * Mật khẩu tạm random (UUID) → gửi email async.
     */
    @PostMapping("/{id}/students/create-accounts")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<BulkCreateAccountsResultResponse> createAccounts(
            @PathVariable Long id,
            @RequestBody(required = false) BulkCreateAccountsRequest request) {
        return ResponseEntity.ok(bulkAccountService.createAccountsForClassroom(
                id, request != null ? request : new BulkCreateAccountsRequest()));
    }

    @DeleteMapping("/{id}/students/{studentId}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> removeStudent(
            @PathVariable Long id,
            @PathVariable Long studentId) {

        classroomService.removeStudentFromClass(id, studentId);
        return ResponseEntity.ok("Đã xóa sinh viên khỏi lớp!");
    }

    /**
     * Giáo viên gán/sửa mã sinh viên cho SV trong lớp.
     */
    @PatchMapping("/{id}/students/{studentId}/student-code")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentInfoResponse> updateStudentCode(
            @PathVariable Long id,
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateStudentCodeRequest request) {
        return ResponseEntity.ok(classroomService.updateStudentCodeInClass(id, studentId, request));
    }
}
