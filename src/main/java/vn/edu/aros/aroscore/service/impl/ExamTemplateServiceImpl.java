package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.CreateExamFromTemplateRequest;
import vn.edu.aros.aroscore.dto.request.ExamConfigRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateUpdateRequest;
import vn.edu.aros.aroscore.dto.response.ExamConfigResponse;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamTemplateResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.ExamType;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.mapper.ExamMapper;
import vn.edu.aros.aroscore.repository.*;
import vn.edu.aros.aroscore.service.ExamTemplateService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamTemplateServiceImpl implements ExamTemplateService {

    private final ExamTemplateRepository examTemplateRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final ExamMapper examMapper;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private ExamTemplate getOwnedActiveTemplate(Long id) {
        return examTemplateRepository.findActiveByIdAndTeacherEmail(id, getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy template hoặc bạn không có quyền!"));
    }

    @Override
    @Transactional
    public ExamTemplateResponse createTemplate(ExamTemplateCreateRequest request) {
        String email = getCurrentUserEmail();
        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giáo viên!"));
        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        List<Question> questions = loadAndValidateQuestions(request.getQuestionIds(), subject.getId(), null);

        ExamTemplate template = ExamTemplate.builder()
                .title(request.getTitle())
                .subject(subject)
                .teacher(teacher)
                .isActive(true)
                .build();

        int order = 1;
        for (Long questionId : request.getQuestionIds()) {
            Question q = questions.stream().filter(x -> x.getId().equals(questionId)).findFirst().orElseThrow();
            Double raw = 1.0;
            if (request.getRawPoints() != null && request.getRawPoints().containsKey(questionId)) {
                raw = request.getRawPoints().get(questionId);
            }
            template.addQuestion(q, order++, raw);
        }

        return toResponse(examTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public ExamTemplateResponse saveExamAsTemplate(Long examId) {
        String email = getCurrentUserEmail();
        Exam exam = examRepository.findByIdAndTeacherEmail(examId, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi hoặc bạn không có quyền!"));

        if (exam.getExamQuestions() == null || exam.getExamQuestions().isEmpty()) {
            throw new RuntimeException("Đề thi chưa có câu hỏi, không thể lưu thành template!");
        }

        ExamTemplate template = ExamTemplate.builder()
                .title(exam.getTitle())
                .subject(exam.getSubject())
                .teacher(exam.getTeacher())
                .isActive(true)
                .build();

        List<ExamQuestion> ordered = exam.getExamQuestions().stream()
                .sorted(Comparator.comparing(ExamQuestion::getQuestionOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        for (ExamQuestion eq : ordered) {
            template.addQuestion(eq.getQuestion(), eq.getQuestionOrder(), eq.getRawPoint());
        }

        return toResponse(examTemplateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamTemplateResponse> getTemplates(Long subjectId, Pageable pageable) {
        String email = getCurrentUserEmail();
        Page<ExamTemplate> page = subjectId != null
                ? examTemplateRepository.findAllActiveByTeacherEmailAndSubjectId(email, subjectId, pageable)
                : examTemplateRepository.findAllActiveByTeacherEmail(email, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamTemplateResponse getTemplateById(Long id) {
        return toResponse(getOwnedActiveTemplate(id));
    }

    @Override
    @Transactional
    public ExamTemplateResponse updateTemplate(Long id, ExamTemplateUpdateRequest request) {
        String email = getCurrentUserEmail();
        ExamTemplate template = getOwnedActiveTemplate(id);

        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        List<Question> questions = loadAndValidateQuestions(request.getQuestionIds(), subject.getId(), null);

        template.setTitle(request.getTitle());
        template.setSubject(subject);

        template.getTemplateQuestions().clear();
        int order = 1;
        for (Long questionId : request.getQuestionIds()) {
            Question q = questions.stream().filter(x -> x.getId().equals(questionId)).findFirst().orElseThrow();
            Double raw = 1.0;
            if (request.getRawPoints() != null && request.getRawPoints().containsKey(questionId)) {
                raw = request.getRawPoints().get(questionId);
            }
            template.addQuestion(q, order++, raw);
        }

        return toResponse(examTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public void softDeleteTemplate(Long id) {
        ExamTemplate template = getOwnedActiveTemplate(id);
        template.setIsActive(false);
        examTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public ExamResponse createExamFromTemplate(Long templateId, CreateExamFromTemplateRequest request) {
        String email = getCurrentUserEmail();
        ExamTemplate template = getOwnedActiveTemplate(templateId);

        if (template.getTemplateQuestions() == null || template.getTemplateQuestions().isEmpty()) {
            throw new RuntimeException("Template chưa có câu hỏi!");
        }

        List<ExamTemplateQuestion> ordered = template.getTemplateQuestions().stream()
                .sorted(Comparator.comparing(ExamTemplateQuestion::getQuestionOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<Long> questionIds = ordered.stream().map(tq -> tq.getQuestion().getId()).toList();
        loadAndValidateQuestions(questionIds, template.getSubject().getId(), request.getExamMode());

        ExamPurpose purpose = request.getPurpose() != null ? request.getPurpose() : ExamPurpose.EXAM;
        Set<Classroom> classrooms = resolveClassrooms(
                request.getClassroomIds(), template.getSubject().getId(), email);

        Exam exam = Exam.builder()
                .title(request.getTitle() != null && !request.getTitle().isBlank()
                        ? request.getTitle() : template.getTitle())
                .duration(request.getDuration())
                .examMode(request.getExamMode())
                .purpose(purpose)
                .subject(template.getSubject())
                .teacher(template.getTeacher())
                .maxScore(request.getMaxScore())
                .status(ExamStatus.DRAFT)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .classrooms(classrooms)
                .sourceTemplate(template)
                .build();

        for (ExamTemplateQuestion tq : ordered) {
            exam.addQuestion(tq.getQuestion(), tq.getQuestionOrder(), tq.getRawPoint());
        }

        ExamConfig config = buildDefaultConfig(exam, request.getConfig(), ordered.size());
        exam.setConfig(config);

        // Luôn DRAFT: GV sinh mã đề rồi mới mở thi (giống flow tạo đề thường).
        if (request.getStatus() != null
                && request.getStatus() != ExamStatus.DRAFT
                && request.getStatus() != ExamStatus.CLOSED
                && request.getStatus() != ExamStatus.COMPLETED) {
            throw new RuntimeException(
                    "Đề tạo từ template phải ở trạng thái DRAFT. Hãy sinh mã đề rồi đổi status để mở thi!");
        }
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }

        if (exam.getStartAt() != null && exam.getEndAt() != null && exam.getEndAt().isBefore(exam.getStartAt())) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        }

        return toExamResponse(examRepository.save(exam));
    }

    private List<Question> loadAndValidateQuestions(List<Long> questionIds, Long subjectId, ExamMode mode) {
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.stream().distinct().count()) {
            throw new RuntimeException("Một số câu hỏi không tồn tại trong hệ thống!");
        }
        for (Question q : questions) {
            if (Boolean.FALSE.equals(q.getIsActive())) {
                throw new RuntimeException("Câu hỏi ID " + q.getId() + " đã bị xóa khỏi ngân hàng!");
            }
            if (!q.getSubject().getId().equals(subjectId)) {
                throw new RuntimeException("Câu hỏi ID " + q.getId() + " không thuộc môn học này!");
            }
            if (mode == ExamMode.OMR_PAPER && q.getType() == QuestionType.MULTIPLE_CHOICE) {
                throw new RuntimeException("Đề OMR không được chứa câu hỏi nhiều đáp án (ID: " + q.getId() + ")");
            }
        }
        return questions;
    }

    private Set<Classroom> resolveClassrooms(List<Long> classroomIds, Long subjectId, String email) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Classroom> classrooms = classroomRepository.findAllByIdsAndSubjectAndLecturer(
                classroomIds, subjectId, email);
        if (classrooms.size() != classroomIds.stream().distinct().count()) {
            throw new RuntimeException("Một số lớp không tồn tại, không thuộc môn này, hoặc bạn không có quyền!");
        }
        for (Classroom classroom : classrooms) {
            if (Boolean.FALSE.equals(classroom.getIsActive())) {
                throw new RuntimeException("Lớp \"" + classroom.getClassName() + "\" đã ngừng hoạt động!");
            }
        }
        return new HashSet<>(classrooms);
    }

    private ExamType toExamType(ExamMode mode) {
        return mode == ExamMode.OMR_PAPER ? ExamType.OMR : ExamType.ONLINE;
    }

    private ExamConfig buildDefaultConfig(Exam exam, ExamConfigRequest request, int questionCount) {
        boolean practice = exam.getPurpose() == ExamPurpose.PRACTICE;
        ExamConfig config = ExamConfig.builder()
                .exam(exam)
                .totalQuestions(questionCount)
                .examType(toExamType(exam.getExamMode()))
                .shuffleQuestions(true)
                .shuffleAnswers(true)
                .paperCount(1)
                .allowEdit(true)
                .showScoreToStudent(true)
                .maxAttempts(practice ? null : 1)
                .timeLimitEnabled(!practice)
                .build();
        applyConfigRequest(config, request, questionCount, exam.getExamMode());
        return config;
    }

    private void applyConfigRequest(ExamConfig config, ExamConfigRequest request, int questionCount, ExamMode mode) {
        config.setTotalQuestions(questionCount);
        config.setExamType(toExamType(mode));
        if (request == null) {
            return;
        }
        if (request.getSemester() != null) {
            config.setSemester(request.getSemester());
        }
        if (request.getAcademicYear() != null) {
            config.setAcademicYear(request.getAcademicYear());
        }
        if (request.getShuffleQuestions() != null) {
            config.setShuffleQuestions(request.getShuffleQuestions());
        }
        if (request.getShuffleAnswers() != null) {
            config.setShuffleAnswers(request.getShuffleAnswers());
        }
        if (request.getPaperCount() != null) {
            config.setPaperCount(request.getPaperCount());
        }
        if (request.getAllowEdit() != null) {
            config.setAllowEdit(request.getAllowEdit());
        }
        if (request.getShowScoreToStudent() != null) {
            config.setShowScoreToStudent(request.getShowScoreToStudent());
        }
        if (request.getMaxAttempts() != null) {
            if (request.getMaxAttempts() < 1) {
                throw new RuntimeException("Số lần làm bài phải >= 1!");
            }
            config.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getTimeLimitEnabled() != null) {
            config.setTimeLimitEnabled(request.getTimeLimitEnabled());
        }
    }

    private ExamTemplateResponse toResponse(ExamTemplate template) {
        List<ExamTemplateQuestion> ordered = template.getTemplateQuestions() == null
                ? List.of()
                : template.getTemplateQuestions().stream()
                .sorted(Comparator.comparing(ExamTemplateQuestion::getQuestionOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        List<Long> questionIds = ordered.stream()
                .map(tq -> tq.getQuestion().getId())
                .collect(Collectors.toList());

        Map<Long, Double> rawPoints = new HashMap<>();
        for (ExamTemplateQuestion tq : ordered) {
            rawPoints.put(tq.getQuestion().getId(), tq.getRawPoint());
        }

        return ExamTemplateResponse.builder()
                .id(template.getId())
                .title(template.getTitle())
                .subjectId(template.getSubject().getId())
                .subjectName(template.getSubject().getSubjectName())
                .teacherEmail(template.getTeacher().getEmail())
                .totalQuestions(questionIds.size())
                .questionIds(questionIds)
                .rawPoints(rawPoints)
                .createdAt(template.getCreatedAt())
                .isActive(template.getIsActive())
                .build();
    }

    private ExamResponse toExamResponse(Exam exam) {
        ExamResponse response = examMapper.toResponse(exam);
        if (exam.getClassrooms() != null) {
            response.setClassroomIds(exam.getClassrooms().stream().map(Classroom::getId).toList());
        } else {
            response.setClassroomIds(List.of());
        }
        if (exam.getConfig() != null) {
            ExamConfig c = exam.getConfig();
            response.setConfig(ExamConfigResponse.builder()
                    .id(c.getId())
                    .semester(c.getSemester())
                    .academicYear(c.getAcademicYear())
                    .totalQuestions(c.getTotalQuestions())
                    .examType(c.getExamType())
                    .shuffleQuestions(c.getShuffleQuestions())
                    .shuffleAnswers(c.getShuffleAnswers())
                    .paperCount(c.getPaperCount())
                    .allowEdit(c.getAllowEdit())
                    .showScoreToStudent(c.getShowScoreToStudent())
                    .maxAttempts(c.getMaxAttempts())
                    .timeLimitEnabled(c.getTimeLimitEnabled())
                    .build());
        }
        if (exam.getSourceTemplate() != null) {
            response.setSourceTemplateId(exam.getSourceTemplate().getId());
        }
        return response;
    }
}
