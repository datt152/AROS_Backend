package vn.edu.aros.aroscore.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamUpdateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.service.ExamService;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody ExamCreateRequest request) {
        ExamResponse response = examService.createExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/versions")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<java.util.List<String>> generateExamVersions(@Valid @RequestBody vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest request) {
        return ResponseEntity.ok(examService.generateExamVersions(request));
    }
    @GetMapping("/{examId}/versions/{versionCode}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<vn.edu.aros.aroscore.dto.response.ExamVersionDetailResponse> getExamVersion(
            @PathVariable Long examId,
            @PathVariable String versionCode) throws JsonProcessingException {
        return ResponseEntity.ok(examService.getExamVersionDetail(examId, versionCode));
    }
    @GetMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<Exam>> getAllExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(examService.getAllExams(pageable));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Exam> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Exam> updateExam(@PathVariable Long id, @Valid @RequestBody ExamUpdateRequest request) {
        return ResponseEntity.ok(examService.updateExam(id, request));
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/take")
//    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<vn.edu.aros.aroscore.dto.response.ExamTakeResponse> takeExam(@PathVariable Long id) {
        return ResponseEntity.ok(examService.takeExam(id));
    }

}
