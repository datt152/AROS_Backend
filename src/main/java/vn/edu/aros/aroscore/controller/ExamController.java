package vn.edu.aros.aroscore.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.service.ExamService;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody ExamCreateRequest request) {
        ExamResponse response = examService.createExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/versions")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<java.util.List<String>> generateExamVersions(@Valid @RequestBody vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest request) {
        return ResponseEntity.ok(examService.generateExamVersions(request));
    }
    @GetMapping("/{examId}/versions/{versionCode}")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<vn.edu.aros.aroscore.dto.response.ExamVersionDetailResponse> getExamVersion(
            @PathVariable Long examId,
            @PathVariable String versionCode) throws JsonProcessingException {
        return ResponseEntity.ok(examService.getExamVersionDetail(examId, versionCode));
    }
}