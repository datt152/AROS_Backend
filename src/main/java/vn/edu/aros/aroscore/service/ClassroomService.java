package vn.edu.aros.aroscore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.ClassroomRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;

public interface ClassroomService {
    @Transactional
    ClassroomResponse createClassroom(ClassroomRequest request);

    Page<ClassroomResponse> getAllClassrooms(Long subjectId, int page, int size);

    ClassroomResponse getClassroomById(Long id);

    @Transactional
    ClassroomResponse updateClassroom(Long id, ClassroomRequest request);

    @Transactional
    void deleteClassroom(Long id);
}
