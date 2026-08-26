package vn.edu.aros.aroscore.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.AssignExamClassroomsRequest;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamUpdateRequest;
import vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.dto.response.ExamGradingResponse;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamStatsResponse;
import vn.edu.aros.aroscore.dto.response.ExamTakeResponse;
import vn.edu.aros.aroscore.dto.response.ExamVersionDetailResponse;
import vn.edu.aros.aroscore.service.ExamService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody ExamCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.createExam(request));
    }

    @PostMapping("/versions")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<String>> generateExamVersions(@Valid @RequestBody ExamVersionCreateRequest request) {
        return ResponseEntity.ok(examService.generateExamVersions(request));
    }

    @GetMapping("/{examId}/versions")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<String>> listVersionCodes(@PathVariable Long examId) {
        return ResponseEntity.ok(examService.listVersionCodes(examId));
    }

    @GetMapping("/{examId}/versions/{versionCode}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamVersionDetailResponse> getExamVersion(
            @PathVariable Long examId,
            @PathVariable String versionCode) throws JsonProcessingException {
        return ResponseEntity.ok(examService.getExamVersionDetail(examId, versionCode));
    }

    @GetMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<ExamResponse>> getAllExams(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(examService.getAllExams(classroomId, pageable));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody ExamUpdateRequest request) {
        return ResponseEntity.ok(examService.updateExam(id, request));
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/classrooms")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> assignClassrooms(
            @PathVariable Long id,
            @Valid @RequestBody AssignExamClassroomsRequest request) {
        return ResponseEntity.ok(examService.assignClassrooms(id, request));
    }

    @GetMapping("/{id}/classrooms")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<ClassroomResponse>> getAssignedClassrooms(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getAssignedClassrooms(id));
    }

    @GetMapping("/{id}/grading")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamGradingResponse> getExamGrading(
            @PathVariable Long id,
            @RequestParam Long classroomId) {
        return ResponseEntity.ok(examService.getExamGrading(id, classroomId));
    }

    @GetMapping("/{id}/stats")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamStatsResponse> getExamStats(
            @PathVariable Long id,
            @RequestParam(required = false) Long classroomId) {
        return ResponseEntity.ok(examService.getExamStats(id, classroomId));
    }

    @GetMapping("/{id}/take")
//    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamTakeResponse> takeExam(@PathVariable Long id) {
        return ResponseEntity.ok(examService.takeExam(id));
    }
}
