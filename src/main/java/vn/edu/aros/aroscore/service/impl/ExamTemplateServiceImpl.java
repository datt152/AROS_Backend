package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.CreateExamFromTemplateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamTemplateUpdateRequest;
import vn.edu.aros.aroscore.dto.request.OnlineExamSettingsRequest;
import vn.edu.aros.aroscore.dto.request.PaperExamSettingsRequest;
import vn.edu.aros.aroscore.dto.request.TopicSelectionRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamTemplateResponse;
import vn.edu.aros.aroscore.dto.response.OnlineExamSettingsResponse;
import vn.edu.aros.aroscore.dto.response.PaperExamSettingsResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.entity.enums.TemplateSelectionMode;
import vn.edu.aros.aroscore.mapper.ExamMapper;
import vn.edu.aros.aroscore.repository.*;
import vn.edu.aros.aroscore.service.ExamTemplateService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final TopicRepository topicRepository;
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
        Subject subject = subjectRepository.findActiveByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        List<Long> questionIds = resolveQuestionIds(request, subject.getId(), email);
        List<Question> questions = loadAndValidateQuestions(questionIds, subject.getId(), null);

        ExamTemplate template = ExamTemplate.builder()
                .title(request.getTitle())
                .subject(subject)
                .teacher(teacher)
                .isActive(true)
                .build();

        int order = 1;
        for (Long questionId : questionIds) {
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
    @Transactional(readOnly = true)
    public ExamTemplateResponse previewTemplate(ExamTemplateCreateRequest request) {
        String email = getCurrentUserEmail();
        Subject subject = subjectRepository.findActiveByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        List<Long> questionIds = resolveQuestionIds(request, subject.getId(), email);
        loadAndValidateQuestions(questionIds, subject.getId(), null);

        Map<Long, Double> rawPoints = new HashMap<>();
        for (Long questionId : questionIds) {
            Double raw = 1.0;
            if (request.getRawPoints() != null && request.getRawPoints().containsKey(questionId)) {
                raw = request.getRawPoints().get(questionId);
            }
            rawPoints.put(questionId, raw);
        }

        return ExamTemplateResponse.builder()
                .title(request.getTitle())
                .subjectId(subject.getId())
                .subjectName(subject.getSubjectName())
                .teacherEmail(email)
                .totalQuestions(questionIds.size())
                .questionIds(questionIds)
                .rawPoints(rawPoints)
                .isActive(true)
                .build();
    }

    private List<Long> resolveQuestionIds(ExamTemplateCreateRequest request, Long subjectId, String email) {
        TemplateSelectionMode mode = request.getSelectionMode() != null
                ? request.getSelectionMode()
                : TemplateSelectionMode.MANUAL;

        if (mode == TemplateSelectionMode.BY_TOPIC) {
            return pickQuestionsByTopic(request.getTopicSelections(), subjectId, email);
        }

        if (request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách câu hỏi rỗng");
        }
        return request.getQuestionIds();
    }

    private List<Long> pickQuestionsByTopic(
            List<TopicSelectionRequest> topicSelections,
            Long subjectId,
            String email) {
        if (topicSelections == null || topicSelections.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn danh sách chủ đề");
        }

        Set<Long> seenTopicIds = new HashSet<>();
        List<Long> pickedIds = new ArrayList<>();
        Set<Long> pickedSet = new HashSet<>();

        for (TopicSelectionRequest sel : topicSelections) {
            if (sel.getTopicId() == null || sel.getCount() == null) {
                throw new IllegalArgumentException("Mỗi chủ đề phải có mã chủ đề và số lượng câu hỏi!");
            }
            if (sel.getCount() < 1) {
                throw new IllegalArgumentException("Số câu của mỗi topic phải lớn hơn 1!");
            }
            if (!seenTopicIds.add(sel.getTopicId())) {
                throw new IllegalArgumentException(
                        "Mã chủ đề " + sel.getTopicId() + " bị trùng trong danh sách được chọn!");
            }

            Topic topic = topicRepository.findActiveByIdAndLecturerEmail(sel.getTopicId(), email)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy chủ đề hoặc bạn không có quyền!"));
            if (!topic.getSubject().getId().equals(subjectId)) {
                throw new IllegalArgumentException(
                        "Chủ đề không thuộc môn học đã chọn!");
            }

            List<Question> pool = questionRepository.findActiveByTopicSubjectAndTeacher(
                    topic.getId(), subjectId, email);
            // Loại câu đã chọn ở topic khác (phòng trường hợp dữ liệu lệch topic)
            List<Question> available = pool.stream()
                    .filter(q -> !pickedSet.contains(q.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));

            int availableCount = available.size();
            if (sel.getCount() > availableCount) {
                throw new IllegalArgumentException(
                        "Số lượng câu hỏi trong chủ đề không đủ để chọn");
            }

            Collections.shuffle(available);
            List<Question> chosen = available.subList(0, sel.getCount());
            for (Question q : chosen) {
                pickedIds.add(q.getId());
                pickedSet.add(q.getId());
            }
        }

        if (pickedIds.isEmpty()) {
            throw new IllegalArgumentException("Tổng số câu sau khi chọn ngẫu nhiên phải lớn hơn 1!");
        }
        return pickedIds;
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

        Subject subject = subjectRepository.findActiveByIdAndLecturerEmail(request.getSubjectId(), email)
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
                .classrooms(classrooms)
                .sourceTemplate(template)
                .build();

        for (ExamTemplateQuestion tq : ordered) {
            exam.addQuestion(tq.getQuestion(), tq.getQuestionOrder(), tq.getRawPoint());
        }

        if (request.getExamMode() == ExamMode.ONLINE) {
            OnlineExamSettings onlineSettings = buildDefaultOnlineSettings(exam, request.getOnlineSettings());
            // Luôn DRAFT: GV sinh mã đề rồi mới mở thi (giống flow tạo đề thường).
            ExamStatus status = onlineSettings.getStatus();
            if (status != ExamStatus.DRAFT
                    && status != ExamStatus.CLOSED
                    && status != ExamStatus.COMPLETED) {
                throw new RuntimeException(
                        "Đề tạo từ template phải ở trạng thái nháp. Hãy sinh mã đề để mở thi!");
            }
            LocalDateTime startAt = onlineSettings.getStartAt();
            LocalDateTime endAt = onlineSettings.getEndAt();
            if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
                throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu!");
            }
            exam.setOnlineSettings(onlineSettings);
        }

        exam.setPaperSettings(buildDefaultPaperSettings(exam, request.getPaperSettings(), ordered.size()));

        return toExamResponse(examRepository.save(exam));
    }

    private List<Question> loadAndValidateQuestions(List<Long> questionIds, Long subjectId, ExamMode mode) {
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.stream().distinct().count()) {
            throw new RuntimeException("Một số câu hỏi không tồn tại trong hệ thống!");
        }
        for (Question q : questions) {
            if (Boolean.FALSE.equals(q.getIsActive())) {
                throw new RuntimeException("Câu hỏi đã bị xóa khỏi ngân hàng!");
            }
            if (!q.getSubject().getId().equals(subjectId)) {
                throw new RuntimeException("Câu hỏi không thuộc môn học này!");
            }
            if (mode == ExamMode.OMR_PAPER && q.getType() == QuestionType.MULTIPLE_CHOICE) {
                throw new RuntimeException("Đề OMR không được chứa câu hỏi nhiều đáp án");
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

    private OnlineExamSettings buildDefaultOnlineSettings(Exam exam, OnlineExamSettingsRequest request) {
        boolean practice = exam.getPurpose() == ExamPurpose.PRACTICE;
        OnlineExamSettings settings = OnlineExamSettings.builder()
                .exam(exam)
                .status(ExamStatus.DRAFT)
                .allowEdit(true)
                .showScoreToStudent(true)
                .maxAttempts(practice ? null : 1)
                .timeLimitEnabled(!practice)
                .build();
        applyOnlineSettingsRequest(settings, request);
        return settings;
    }

    private void applyOnlineSettingsRequest(OnlineExamSettings settings, OnlineExamSettingsRequest request) {
        if (request == null) {
            return;
        }
        if (request.getStatus() != null) {
            settings.setStatus(request.getStatus());
        }
        if (request.getStartAt() != null) {
            settings.setStartAt(request.getStartAt());
        }
        if (request.getEndAt() != null) {
            settings.setEndAt(request.getEndAt());
        }
        if (request.getAllowEdit() != null) {
            settings.setAllowEdit(request.getAllowEdit());
        }
        if (request.getShowScoreToStudent() != null) {
            settings.setShowScoreToStudent(request.getShowScoreToStudent());
        }
        if (request.getMaxAttempts() != null) {
            if (request.getMaxAttempts() < 1) {
                throw new RuntimeException("Số lần làm bài phải lớn hơn 1!");
            }
            settings.setMaxAttempts(request.getMaxAttempts());
        }
        if (request.getTimeLimitEnabled() != null) {
            settings.setTimeLimitEnabled(request.getTimeLimitEnabled());
        }
    }

    private PaperExamSettings buildDefaultPaperSettings(Exam exam, PaperExamSettingsRequest request, int questionCount) {
        PaperExamSettings settings = PaperExamSettings.builder()
                .exam(exam)
                .totalQuestions(questionCount)
                .shuffleQuestions(true)
                .shuffleAnswers(true)
                .paperCount(1)
                .build();
        applyPaperSettingsRequest(settings, request, questionCount);
        return settings;
    }

    private void applyPaperSettingsRequest(PaperExamSettings settings, PaperExamSettingsRequest request, int questionCount) {
        settings.setTotalQuestions(questionCount);
        if (request == null) {
            return;
        }
        if (request.getExamDate() != null) {
            settings.setExamDate(request.getExamDate());
        }
        if (request.getSemester() != null) {
            settings.setSemester(request.getSemester());
        }
        if (request.getAcademicYear() != null) {
            settings.setAcademicYear(request.getAcademicYear());
        }
        if (request.getShuffleQuestions() != null) {
            settings.setShuffleQuestions(request.getShuffleQuestions());
        }
        if (request.getShuffleAnswers() != null) {
            settings.setShuffleAnswers(request.getShuffleAnswers());
        }
        if (request.getPaperCount() != null) {
            settings.setPaperCount(request.getPaperCount());
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
        if (exam.getOnlineSettings() != null) {
            OnlineExamSettings s = exam.getOnlineSettings();
            response.setOnlineSettings(OnlineExamSettingsResponse.builder()
                    .id(s.getId())
                    .status(s.getStatus())
                    .startAt(s.getStartAt())
                    .endAt(s.getEndAt())
                    .allowEdit(s.getAllowEdit())
                    .showScoreToStudent(s.getShowScoreToStudent())
                    .maxAttempts(s.getMaxAttempts())
                    .timeLimitEnabled(s.getTimeLimitEnabled())
                    .build());
            response.setStatus(s.getStatus());
            response.setStartAt(s.getStartAt());
            response.setEndAt(s.getEndAt());
        }
        if (exam.getPaperSettings() != null) {
            PaperExamSettings p = exam.getPaperSettings();
            response.setPaperSettings(PaperExamSettingsResponse.builder()
                    .id(p.getId())
                    .examDate(p.getExamDate())
                    .semester(p.getSemester())
                    .academicYear(p.getAcademicYear())
                    .totalQuestions(p.getTotalQuestions())
                    .shuffleQuestions(p.getShuffleQuestions())
                    .shuffleAnswers(p.getShuffleAnswers())
                    .paperCount(p.getPaperCount())
                    .build());
        }
        if (exam.getSourceTemplate() != null) {
            response.setSourceTemplateId(exam.getSourceTemplate().getId());
        }
        return response;
    }
}
