package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.OmrReviewRequest;
import vn.edu.aros.aroscore.dto.response.OmrSheetResponse;
import vn.edu.aros.aroscore.service.OmrSheetService;

@RestController
@RequestMapping("/api/v1/omr-sheets")
@RequiredArgsConstructor
public class OmrSheetController {

    private final OmrSheetService omrSheetService;

    @GetMapping("/{id}")
    public ResponseEntity<OmrSheetResponse> getSheet(@PathVariable Long id) {
        return ResponseEntity.ok(omrSheetService.getSheet(id));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<OmrSheetResponse> reviewSheet(
            @PathVariable Long id,
            @Valid @RequestBody OmrReviewRequest request) {
        return ResponseEntity.ok(omrSheetService.reviewAndRegrade(id, request));
    }
}
