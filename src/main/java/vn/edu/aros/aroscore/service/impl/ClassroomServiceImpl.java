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
import vn.edu.aros.aroscore.dto.request.ClassroomUpdateRequest;
import vn.edu.aros.aroscore.dto.request.EnrollStudentRequest;
import vn.edu.aros.aroscore.dto.request.UpdateStudentCodeRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.dto.response.StudentInfoResponse;
import vn.edu.aros.aroscore.entity.Account;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.UserRole;
import vn.edu.aros.aroscore.mapper.ClassroomMapper;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.ClassroomService;
import vn.edu.aros.aroscore.service.StudentCodeService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassroomServiceImpl implements ClassroomService {

    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ClassroomMapper classroomMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentCodeService studentCodeService;

    @Transactional
    @Override
    public ClassroomResponse createClassroom(ClassroomRequest request) {
        String email = getCurrentUserEmail();

        Subject subject = subjectRepository.findActiveByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc môn học đã bị ẩn!"));

        if (classroomRepository.existsByClassNameAndSubjectId(request.getClassName(), request.getSubjectId())) {
            throw new RuntimeException("Tên lớp học đã tồn tại trong môn học này!");
        }

        Classroom classroom = classroomMapper.toEntity(request);
        classroom.setSubject(subject);

        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Override
    public Page<ClassroomResponse> getAllClassrooms(Long subjectId, int page, int size, boolean includeInactive) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size, Sort.by("className").ascending());
        if (subjectId != null) {
            return classroomRepository.findAllBySubjectIdAndLecturerEmail(subjectId, email, includeInactive, pageable)
                    .map(classroomMapper::toResponse);
        }
        return classroomRepository.findAllByLecturerEmail(email, includeInactive, pageable)
                .map(classroomMapper::toResponse);
    }

    @Override
    public ClassroomResponse getClassroomById(Long id) {
        return classroomMapper.toResponse(getAuthorizedClassroom(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentInfoResponse> getClassroomStudents(Long classId) {
        Classroom classroom = classroomRepository.findByIdWithStudents(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));

        assertLecturerAccess(classroom);

        if (classroom.getStudents() == null || classroom.getStudents().isEmpty()) {
            return Collections.emptyList();
        }

        return classroom.getStudents().stream()
                .map(this::toStudentInfoResponse)
                .toList();
    }

    @Transactional
    @Override
    public ClassroomResponse updateClassroom(Long id, ClassroomUpdateRequest request) {
        Classroom classroom = getAuthorizedClassroom(id);

        if (!classroom.getClassName().equals(request.getClassName())
                && classroomRepository.existsByClassNameAndSubjectIdAndIdNot(
                        request.getClassName(), classroom.getSubject().getId(), classroom.getId())) {
            throw new RuntimeException("Tên lớp học mới đã tồn tại trong môn học này!");
        }

        classroomMapper.updateEntityFromUpdateRequest(request, classroom);
        return classroomMapper.toResponse(classroomRepository.save(classroom));
    }

    @Transactional
    @Override
    public void deleteClassroom(Long id) {
        Classroom classroom = getAuthorizedClassroom(id);

        if (Boolean.FALSE.equals(classroom.getIsActive())) {
            throw new RuntimeException("Lớp học đã được ẩn trước đó!");
        }

        classroom.setIsActive(false);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void enrollStudents(Long classId, EnrollStudentRequest request) {
        Classroom classroom = getAuthorizedClassroom(classId);
        assertClassroomActive(classroom);
        String teacherEmail = getCurrentUserEmail();

        List<User> students = resolveEnrollableStudents(request.getStudentEmails(), teacherEmail);

        // Bỏ qua SV đã nằm trong lớp (tránh báo trùng chính họ)
        List<User> toEnroll = students.stream()
                .filter(s -> classroom.getStudents() == null || !classroom.getStudents().contains(s))
                .toList();

        studentCodeService.assertNoStudentCodeConflictInBatch(toEnroll);
        for (User student : toEnroll) {
            studentCodeService.assertNoStudentCodeConflictInClassroom(classId, student);
        }

        if (classroom.getStudents() == null) {
            classroom.setStudents(new HashSet<>());
        }

        classroom.getStudents().addAll(toEnroll);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        Classroom classroom = getAuthorizedClassroom(classId);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên!"));

        Account account = student.getAccount();
        if (account != null && account.getRole() != UserRole.STUDENT) {
            throw new RuntimeException("Chỉ có thể xóa sinh viên khỏi lớp!");
        }

        if (classroom.getStudents() == null || !classroom.getStudents().contains(student)) {
            throw new RuntimeException("Sinh viên không thuộc lớp học này!");
        }

        classroom.getStudents().remove(student);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassroomResponse> getMyClassrooms(Long subjectId, int page, int size) {
        User student = userRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh!"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("className").ascending());
        Page<Classroom> classrooms = subjectId != null
                ? classroomRepository.findAllActiveByStudentIdAndSubjectId(student.getId(), subjectId, pageable)
                : classroomRepository.findAllActiveByStudentId(student.getId(), pageable);
        return classrooms.map(classroomMapper::toResponse);
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Classroom getAuthorizedClassroom(Long id) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học!"));
        assertLecturerAccess(classroom);
        return classroom;
    }

    private void assertLecturerAccess(Classroom classroom) {
        if (!classroom.getSubject().getLecturer().getEmail().equals(getCurrentUserEmail())) {
            throw new RuntimeException("Bạn không có quyền truy cập lớp học này!");
        }
    }

    private void assertClassroomActive(Classroom classroom) {
        if (Boolean.FALSE.equals(classroom.getIsActive())) {
            throw new RuntimeException("Lớp học đã ngừng hoạt động, không thể thêm sinh viên!");
        }
    }

    private List<User> resolveEnrollableStudents(List<String> requestedEmails, String teacherEmail) {
        boolean includesSelf = requestedEmails.stream()
                .anyMatch(email -> email.equalsIgnoreCase(teacherEmail));
        if (includesSelf) {
            throw new RuntimeException("Bạn không thể tự thêm bản thân vào lớp học!");
        }

        List<User> users = userRepository.findAllByEmailInWithAccount(requestedEmails);

        if (users.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sinh viên nào hợp lệ trong hệ thống với các email đã cung cấp!");
        }

        Set<String> foundEmails = users.stream()
                .map(user -> user.getEmail().toLowerCase())
                .collect(Collectors.toSet());

        List<String> missingEmails = requestedEmails.stream()
                .filter(email -> !foundEmails.contains(email.toLowerCase()))
                .toList();
        if (!missingEmails.isEmpty()) {
            throw new RuntimeException("Không tìm thấy email trong hệ thống: " + String.join(", ", missingEmails));
        }

        users.forEach(this::assertStudentRole);
        users.forEach(this::assertActiveStudent);

        return users;
    }

    private void assertStudentRole(User user) {
        Account account = user.getAccount();
        if (account == null || account.getRole() != UserRole.STUDENT) {
            throw new RuntimeException("Chỉ có thể thêm sinh viên vào lớp. Email không hợp lệ: " + user.getEmail());
        }
    }

    private void assertActiveStudent(User user) {
        if (!user.isActive()) {
            throw new RuntimeException("Sinh viên không còn hoạt động: " + user.getEmail());
        }
    }

    @Override
    @Transactional
    public StudentInfoResponse updateStudentCodeInClass(
            Long classId, Long studentId, UpdateStudentCodeRequest request) {
        Classroom classroom = getAuthorizedClassroom(classId);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên!"));

        if (classroom.getStudents() == null || !classroom.getStudents().contains(student)) {
            throw new RuntimeException("Sinh viên không thuộc lớp học này!");
        }

        String normalized = studentCodeService.validateForAllClassroomsOfUser(
                request.getStudentCode(), student.getId());

        student.setStudentCode(normalized);
        userRepository.save(student);
        return toStudentInfoResponse(student);
    }

    private StudentInfoResponse toStudentInfoResponse(User user) {
        String code = user.getStudentCode();
        boolean missing = code == null || code.isBlank();
        boolean hasAccount = user.getAccount() != null;
        return StudentInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentCode(code)
                .missingStudentCode(missing)
                .hasAccount(hasAccount)
                .build();
    }
}
