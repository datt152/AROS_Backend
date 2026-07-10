package vn.edu.aros.aroscore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.aros.aroscore.dto.request.SubjectRequest;
import vn.edu.aros.aroscore.dto.response.SubjectResponse;

public interface SubjectService {
    SubjectResponse createSubject(SubjectRequest request);
    SubjectResponse getSubjectById(Long id);


    Page<SubjectResponse> getAllSubjects(int page, int size);

    SubjectResponse updateSubject(Long id, SubjectRequest request);
    void deleteSubject(Long id);
}
