package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.CreateExamSessionRequest;
import vn.edu.aros.aroscore.dto.response.ExamSessionResponse;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.ExamSession;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamSessionStatus;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.ExamSessionRepository;
import vn.edu.aros.aroscore.repository.OMRFileRepository;
import vn.edu.aros.aroscore.service.ExamSessionService;
import vn.edu.aros.aroscore.service.omr.OmrExamValidator;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final OMRFileRepository omrFileRepository;
    private final OmrExamValidator omrExamValidator;

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public ExamSessionResponse createSession(CreateExamSessionRequest request) {
        Exam exam = examRepository.findByIdAndTeacherEmail(request.getExamId(), currentEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi hoặc bạn không có quyền!"));

        if (exam.getExamMode() != ExamMode.OMR_PAPER) {
            throw new RuntimeException("Chỉ đề OMR mới tạo được phiên chấm phiếu!");
        }
        omrExamValidator.validateOmrExam(exam);

        ExamSession session = ExamSession.builder()
                .exam(exam)
                .name(request.getName())
                .status(ExamSessionStatus.OPEN)
                .build();
        return toResponse(examSessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSessionResponse getSession(Long id) {
        return toResponse(getOwnedSession(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSessionResponse> listByExam(Long examId) {
        return examSessionRepository.findAllByExamIdAndTeacherEmail(examId, currentEmail()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExamSessionResponse updateStatus(Long id, ExamSessionStatus status) {
        ExamSession session = getOwnedSession(id);
        session.setStatus(status);
        return toResponse(examSessionRepository.save(session));
    }

    private ExamSession getOwnedSession(Long id) {
        return examSessionRepository.findByIdAndTeacherEmail(id, currentEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chấm hoặc bạn không có quyền!"));
    }

    private ExamSessionResponse toResponse(ExamSession session) {
        int count = omrFileRepository.findByExamSessionIdOrderByUploadTimeDesc(session.getId()).size();
        return ExamSessionResponse.builder()
                .id(session.getId())
                .examId(session.getExam().getId())
                .examTitle(session.getExam().getTitle())
                .name(session.getName())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .sheetCount(count)
                .build();
    }
}
