package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.CreateExamSessionRequest;
import vn.edu.aros.aroscore.dto.response.ExamSessionResponse;
import vn.edu.aros.aroscore.entity.enums.ExamSessionStatus;

import java.util.List;

public interface ExamSessionService {

    ExamSessionResponse createSession(CreateExamSessionRequest request);

    ExamSessionResponse getSession(Long id);

    List<ExamSessionResponse> listByExam(Long examId);

    ExamSessionResponse updateStatus(Long id, ExamSessionStatus status);
}
