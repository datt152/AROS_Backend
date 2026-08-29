package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.aros.aroscore.dto.request.TopicRequest;
import vn.edu.aros.aroscore.dto.response.TopicResponse;
import vn.edu.aros.aroscore.service.TopicService;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PostMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TopicResponse> createTopic(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.createTopic(request));
    }

    @GetMapping
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<TopicResponse>> getTopics(
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(topicService.getTopicsBySubject(subjectId, page, size, includeInactive));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TopicResponse> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getTopicById(id));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TopicResponse> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(topicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> softDeleteTopic(@PathVariable Long id) {
        topicService.softDeleteTopic(id);
        return ResponseEntity.ok("Đã xóa chủ đề!");
    }
}
