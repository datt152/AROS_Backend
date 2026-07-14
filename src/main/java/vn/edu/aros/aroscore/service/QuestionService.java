package vn.edu.aros.aroscore.service;

import org.springframework.data.domain.Page;
import vn.edu.aros.aroscore.dto.request.QuestionRequest;
import vn.edu.aros.aroscore.dto.response.QuestionResponse;

public interface QuestionService {
    QuestionResponse createQuestion(QuestionRequest request);
    Page<QuestionResponse> getAllQuestions(Long subjectId, int page, int size);
}
