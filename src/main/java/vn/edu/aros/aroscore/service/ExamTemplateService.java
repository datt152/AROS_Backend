package vn.edu.aros.aroscore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.aros.aroscore.dto.request.CreateExamFromTemplateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateUpdateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamTemplateResponse;

public interface ExamTemplateService {

    ExamTemplateResponse createTemplate(ExamTemplateCreateRequest request);

    ExamTemplateResponse saveExamAsTemplate(Long examId);

    Page<ExamTemplateResponse> getTemplates(Long subjectId, Pageable pageable);

    ExamTemplateResponse getTemplateById(Long id);

    ExamTemplateResponse updateTemplate(Long id, ExamTemplateUpdateRequest request);

    void softDeleteTemplate(Long id);

    ExamResponse createExamFromTemplate(Long templateId, CreateExamFromTemplateRequest request);
}
