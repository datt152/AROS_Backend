package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.SubjectRequest;
import vn.edu.aros.aroscore.dto.response.SubjectResponse;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.mapper.SubjectMapper;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.SubjectService;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectMapper subjectMapper;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        String email = getCurrentUserEmail();

        // Check trùng tên môn học NHƯNG chỉ trong phạm vi của giáo viên này thôi
        if (subjectRepository.existsBySubjectNameAndLecturerEmail(request.getSubjectName(), email)) {
            throw new RuntimeException("Bạn đã có môn học với tên này rồi!");
        }

        Subject subject = subjectMapper.toEntity(request);

        User lecturer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giảng viên!"));

        subject.setLecturer(lecturer);

        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(savedSubject);
    }

    @Override
    public SubjectResponse getSubjectById(Long id) {
        String email = getCurrentUserEmail();

        // Đảm bảo chỉ lấy được môn học nếu môn đó do chính mình tạo
        Subject subject = subjectRepository.findByIdAndLecturerEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc bạn không có quyền xem!"));

        return subjectMapper.toResponse(subject);
    }

    @Override
    public Page<SubjectResponse> getAllSubjects(int page, int size) {
        String email = getCurrentUserEmail();

        Pageable pageable = PageRequest.of(page, size, Sort.by("subjectName").ascending());

        // Lọc danh sách môn học theo đúng giáo viên đang đăng nhập
        return subjectRepository.findAllByLecturerEmail(email, pageable)
                .map(subjectMapper::toResponse);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        String email = getCurrentUserEmail();

        Subject subject = subjectRepository.findByIdAndLecturerEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc bạn không có quyền sửa!"));

        if (!subject.getSubjectName().equalsIgnoreCase(request.getSubjectName())
                && subjectRepository.existsBySubjectNameAndLecturerEmail(request.getSubjectName(), email)) {
            throw new RuntimeException("Tên môn học mới bị trùng với một môn khác của bạn!");
        }

        subjectMapper.updateEntityFromRequest(request, subject);

        Subject updatedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(updatedSubject);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        String email = getCurrentUserEmail();

        Subject subject = subjectRepository.findByIdAndLecturerEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc bạn không có quyền xóa!"));

        subjectRepository.delete(subject);
    }
}