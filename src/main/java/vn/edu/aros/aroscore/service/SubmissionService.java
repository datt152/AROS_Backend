package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.SubmissionRequest;
import vn.edu.aros.aroscore.dto.response.StudentSubmissionItemResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionDetailResponse;
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;

import java.util.List;

public interface SubmissionService {
    SubmissionResponse submitExam(SubmissionRequest request);

    SubmissionDetailResponse getSubmissionDetail(Long submissionId);

    List<StudentSubmissionItemResponse> getMySubmissions(Long examId);
}