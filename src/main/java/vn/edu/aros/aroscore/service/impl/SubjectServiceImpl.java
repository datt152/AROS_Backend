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



        Subject subject = subjectRepository.findByIdAndLecturerEmail(id, email)

                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc bạn không có quyền xem!"));



        return subjectMapper.toResponse(subject);

    }



    @Override

    public Page<SubjectResponse> getAllSubjects(int page, int size, boolean includeInactive) {

        String email = getCurrentUserEmail();

        Pageable pageable = PageRequest.of(page, size, Sort.by("subjectName").ascending());



        return subjectRepository.findAllByLecturerEmail(email, includeInactive, pageable)

                .map(subjectMapper::toResponse);

    }



    @Override

    @Transactional

    public SubjectResponse updateSubject(Long id, SubjectRequest request) {

        String email = getCurrentUserEmail();



        Subject subject = subjectRepository.findByIdAndLecturerEmail(id, email)

                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc bạn không có quyền sửa!"));



        if (!subject.getSubjectName().equalsIgnoreCase(request.getSubjectName())

                && subjectRepository.existsBySubjectNameAndLecturerEmailAndIdNot(request.getSubjectName(), email, id)) {

            throw new RuntimeException("Tên môn học mới bị trùng với một môn khác của bạn!");

        }



        subjectMapper.updateEntityFromRequest(request, subject);

        if (request.getIsActive() != null) {
            subject.setIsActive(request.getIsActive());
        }

        Subject updatedSubject = subjectRepository.save(subject);

        return subjectMapper.toResponse(updatedSubject);

    }



    @Override

    @Transactional

    public void deleteSubject(Long id) {

        String email = getCurrentUserEmail();



        Subject subject = subjectRepository.findByIdAndLecturerEmail(id, email)

                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học hoặc bạn không có quyền xóa!"));



        if (Boolean.FALSE.equals(subject.getIsActive())) {

            throw new RuntimeException("Môn học đã được ẩn trước đó!");

        }



        subject.setIsActive(false);

        subjectRepository.save(subject);

    }

}

