package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.ClassroomRequest;
import vn.edu.aros.aroscore.dto.request.EnrollStudentRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.dto.response.UserResponse;
import vn.edu.aros.aroscore.service.ClassroomService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @PostMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> createClassroom(@Valid @RequestBody ClassroomRequest request) {
        return new ResponseEntity<>(classroomService.createClassroom(request), HttpStatus.CREATED);
    }

    @GetMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<ClassroomResponse>> getAllClassrooms(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(classroomService.getAllClassrooms(subjectId, page, size));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> getClassroomById(@PathVariable Long id) {
        return ResponseEntity.ok(classroomService.getClassroomById(id));
    }

    @GetMapping("/{id}/students")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<UserResponse>> getClassroomStudents(@PathVariable Long id) {
        return ResponseEntity.ok(classroomService.getClassroomStudents(id));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ClassroomResponse> updateClassroom(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomRequest request) {
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

    @DeleteMapping("/{id}/students/{studentId}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> removeStudent(
            @PathVariable Long id,
            @PathVariable Long studentId) {

        classroomService.removeStudentFromClass(id, studentId);
        return ResponseEntity.ok("Đã xóa sinh viên khỏi lớp!");
    }
}
