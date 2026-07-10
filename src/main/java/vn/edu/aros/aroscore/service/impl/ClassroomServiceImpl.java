package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.ClassroomRequest;
import vn.edu.aros.aroscore.dto.request.EnrollStudentRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.mapper.ClassroomMapper;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.ClassroomService;

import java.util.List;

@Service
public class ClassroomServiceImpl implements ClassroomService {

    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ClassroomMapper classroomMapper;
    @Autowired
    private UserRepository userRepository;
    @Transactional
    @Override
    public ClassroomResponse createClassroom(ClassroomRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học!"));

        if (!subject.getLecturer().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền quản lý môn học này!");
        }

        if (classroomRepository.existsByClassNameAndSubjectId(request.getClassName(), request.getSubjectId())) {
            throw new RuntimeException("Tên lớp học đã tồn tại trong môn học này!");
        }

        Classroom classroom = classroomMapper.toEntity(request);
        classroom.setSubject(subject);
        // Khi tạo mới, Set<User> students sẽ mặc định là null hoặc empty (phù hợp logic)

        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Override
    public Page<ClassroomResponse> getAllClassrooms(Long subjectId, int page, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("className").ascending());
        if (subjectId != null) {
            return classroomRepository.findAllBySubjectIdAndLecturerEmail(subjectId, email, pageable)
                    .map(classroomMapper::toResponse);
        }
        return classroomRepository.findAllByLecturerEmail(email, pageable)
                .map(classroomMapper::toResponse);
    }

    @Override
    public ClassroomResponse getClassroomById(Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        if (!classroom.getSubject().getLecturer().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền truy cập lớp học này!");
        }
        return classroomMapper.toResponse(classroom);
    }

    @Transactional
    @Override
    public ClassroomResponse updateClassroom(Long id, ClassroomRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        if (!classroom.getSubject().getLecturer().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa lớp học này!");
        }

        if (!classroom.getClassName().equals(request.getClassName()) &&
                classroomRepository.existsByClassNameAndSubjectId(request.getClassName(), classroom.getSubject().getId())) {
            throw new RuntimeException("Tên lớp học mới đã tồn tại trong môn học này!");
        }

        classroomMapper.updateEntityFromRequest(request, classroom);
        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Transactional
    @Override
    public void deleteClassroom(Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        if (!classroom.getSubject().getLecturer().getEmail().equals(email)) {
            throw new RuntimeException("Bạn không có quyền xóa lớp học này!");
        }
        classroomRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void enrollStudents(Long classId, EnrollStudentRequest request) {
        String teacherEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        // 1. Kiểm tra bảo mật: Lớp này có phải của giáo viên đang thao tác không?
        if (!classroom.getSubject().getLecturer().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Bạn không có quyền thêm sinh viên vào lớp học này!");
        }

        // 2. Truy xuất danh sách sinh viên từ DB dựa trên list email gửi lên
        List<User> students = userRepository.findAllByEmailIn(request.getStudentEmails());

        if (students.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sinh viên nào hợp lệ trong hệ thống với các email đã cung cấp!");
        }

        // 3. Add vào Set (Tự động loại bỏ trùng lặp nếu sinh viên đã có trong lớp)
        classroom.getStudents().addAll(students);

        // 4. Lưu lại (Hibernate sẽ tự động insert vào bảng trung gian classroom_students)
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        String teacherEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        if (!classroom.getSubject().getLecturer().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên lớp học này!");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên!"));

        // Xóa sinh viên khỏi Set
        classroom.getStudents().remove(student);

        classroomRepository.save(classroom);
    }
}