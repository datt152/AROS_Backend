package vn.edu.aros.aroscore.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.request.QuestionRequest;
import vn.edu.aros.aroscore.dto.response.QuestionImportResult;
import vn.edu.aros.aroscore.dto.response.QuestionResponse;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.QuestionImportService;
import vn.edu.aros.aroscore.service.QuestionService;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionImportService questionImportService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody QuestionRequest request) {
        return new ResponseEntity<>(questionService.createQuestion(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<QuestionResponse>> getAllQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(questionService.getAllQuestions(subjectId, topicId, page, size));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> softDeleteQuestion(@PathVariable Long id) {
        questionService.softDeleteQuestion(id);
        return ResponseEntity.ok("Đã xóa câu hỏi khỏi ngân hàng!");
    }

    /**
     * Import hàng loạt câu hỏi từ file Excel vào một Topic cụ thể
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<QuestionImportResult> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("topicId") Long topicId) {
        
        // Lấy teacher ID từ security context
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giảng viên"));
        
        QuestionImportResult result = questionImportService.importQuestionsFromExcel(
            file,
            topicId,
            teacher.getId()
        );
        
        return ResponseEntity.ok(result);
    }
}
