package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.request.SubmissionRequest;
import vn.edu.aros.aroscore.dto.response.StudentSubmissionItemResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionDetailResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;
import vn.edu.aros.aroscore.service.SubmissionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
//    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionResponse> submitExam(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(submissionService.submitExam(request));
    }

    @GetMapping("/my")
//    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentSubmissionItemResponse>> getMySubmissions(
            @RequestParam(required = false) Long examId) {
        return ResponseEntity.ok(submissionService.getMySubmissions(examId));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    public ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionDetail(id));
    }
}
