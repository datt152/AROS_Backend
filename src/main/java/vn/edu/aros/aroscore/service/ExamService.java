package vn.edu.aros.aroscore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamVersionDetailResponse;

import java.util.List;

public interface ExamService {
    ExamResponse createExam(ExamCreateRequest request);
    List<String> generateExamVersions(ExamVersionCreateRequest request);
    ExamVersionDetailResponse getExamVersionDetail(Long examId, String versionCode) throws JsonProcessingException;
}