package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        if (subjectRepository.existsBySubjectName(request.getSubjectName())) {
            throw new RuntimeException("Tên môn học đã tồn tại!");
        }

        Subject subject = subjectMapper.toEntity(request);

        if (request.getLecturerId() != null) {
            User lecturer = userRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên với ID: " + request.getLecturerId()));
            subject.setLecturer(lecturer);
        }
//        subject.setLecturer(null); // Set lecturer to null when creating a new subject

        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(savedSubject);
    }

    @Override
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học với ID: " + id));
        return subjectMapper.toResponse(subject);
    }

    @Override
    public Page<SubjectResponse> getAllSubjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("subjectName").ascending());

        return subjectRepository.findAll(pageable)
                .map(subjectMapper::toResponse);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học với ID: " + id));

        if (!subject.getSubjectName().equalsIgnoreCase(request.getSubjectName())
                && subjectRepository.existsBySubjectName(request.getSubjectName())) {
            throw new RuntimeException("Tên môn học mới đã tồn tại hệ thống!");
        }

        subjectMapper.updateEntityFromRequest(request, subject);

        if (request.getLecturerId() != null) {
            User lecturer = userRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giảng viên với ID: " + request.getLecturerId()));
            subject.setLecturer(lecturer);
        } else {
            subject.setLecturer(null);
        }

        Subject updatedSubject = subjectRepository.save(subject);
        return subjectMapper.toResponse(updatedSubject);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy môn học để xóa!");
        }
        subjectRepository.deleteById(id);
    }
}