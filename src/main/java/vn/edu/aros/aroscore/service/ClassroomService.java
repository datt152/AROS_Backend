package vn.edu.aros.aroscore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.ClassroomRequest;
import vn.edu.aros.aroscore.dto.request.ClassroomUpdateRequest;
import vn.edu.aros.aroscore.dto.request.EnrollStudentRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.dto.response.StudentInfoResponse;

import java.util.List;

public interface ClassroomService {
    @Transactional
    ClassroomResponse createClassroom(ClassroomRequest request);

    Page<ClassroomResponse> getAllClassrooms(Long subjectId, int page, int size, boolean includeInactive);

    ClassroomResponse getClassroomById(Long id);

    @Transactional
    ClassroomResponse updateClassroom(Long id, ClassroomUpdateRequest request);

    @Transactional
    void deleteClassroom(Long id);

    void enrollStudents(Long classId, EnrollStudentRequest request);

    void removeStudentFromClass(Long classId, Long studentId);

    List<StudentInfoResponse> getClassroomStudents(Long classId);

    Page<ClassroomResponse> getMyClassrooms(Long subjectId, int page, int size);
}
