package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.request.CreateExamSessionRequest;
import vn.edu.aros.aroscore.dto.request.OmrReviewRequest;
import vn.edu.aros.aroscore.dto.response.ExamSessionResponse;
import vn.edu.aros.aroscore.dto.response.OmrSheetResponse;
import vn.edu.aros.aroscore.entity.enums.ExamSessionStatus;
import vn.edu.aros.aroscore.service.ExamSessionService;
import vn.edu.aros.aroscore.service.OmrSheetService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService examSessionService;
    private final OmrSheetService omrSheetService;

    @PostMapping
    public ResponseEntity<ExamSessionResponse> createSession(
            @Valid @RequestBody CreateExamSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examSessionService.createSession(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamSessionResponse> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(examSessionService.getSession(id));
    }

    @GetMapping
    public ResponseEntity<List<ExamSessionResponse>> listByExam(@RequestParam Long examId) {
        return ResponseEntity.ok(examSessionService.listByExam(examId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ExamSessionResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ExamSessionStatus status) {
        return ResponseEntity.ok(examSessionService.updateStatus(id, status));
    }

    @PostMapping(value = "/{sessionId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OmrSheetResponse> uploadSheet(
            @PathVariable Long sessionId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(omrSheetService.uploadAndScan(sessionId, file));
    }

    @GetMapping("/{sessionId}/submissions")
    public ResponseEntity<List<OmrSheetResponse>> listSheets(@PathVariable Long sessionId) {
        return ResponseEntity.ok(omrSheetService.listBySession(sessionId));
    }
}
