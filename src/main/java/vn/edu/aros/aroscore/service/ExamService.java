package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;

public interface ExamService {
    ExamResponse createExam(ExamCreateRequest request);
}