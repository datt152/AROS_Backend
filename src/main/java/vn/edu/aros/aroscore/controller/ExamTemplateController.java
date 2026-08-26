package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.CreateExamFromTemplateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateUpdateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamTemplateResponse;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.service.ExamTemplateService;

@RestController
@RequestMapping("/api/v1/exam-templates")
@RequiredArgsConstructor
public class ExamTemplateController {

    private final ExamTemplateService examTemplateService;

    @PostMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamTemplateResponse> createTemplate(
            @Valid @RequestBody ExamTemplateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examTemplateService.createTemplate(request));
    }

    @GetMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<ExamTemplateResponse>> getTemplates(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) ExamPurpose purpose,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(examTemplateService.getTemplates(subjectId, purpose, pageable));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamTemplateResponse> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(examTemplateService.getTemplateById(id));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ExamTemplateUpdateRequest request) {
        return ResponseEntity.ok(examTemplateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> deleteTemplate(@PathVariable Long id) {
        examTemplateService.softDeleteTemplate(id);
        return ResponseEntity.ok("Đã xóa template!");
    }

    @PostMapping("/{id}/create-exam")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ExamResponse> createExamFromTemplate(
            @PathVariable Long id,
            @Valid @RequestBody CreateExamFromTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examTemplateService.createExamFromTemplate(id, request));
    }
}
