package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.SubmissionRequest;
import vn.edu.aros.aroscore.dto.response.SubmissionResponse;

public interface SubmissionService {
    SubmissionResponse submitExam(SubmissionRequest request);
}