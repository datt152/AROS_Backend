package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.request.SubmissionRequest;
import vn.edu.aros.aroscore.dto.response.SubmissionDetailResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;
import vn.edu.aros.aroscore.service.SubmissionService;

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

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionDetail(id));
    }
}
