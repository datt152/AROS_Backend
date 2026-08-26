package vn.edu.aros.aroscore.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.AnswerOptionRequest;
import vn.edu.aros.aroscore.dto.request.QuestionRequest;
import vn.edu.aros.aroscore.dto.response.QuestionResponse;
import vn.edu.aros.aroscore.entity.AnswerOption;
import vn.edu.aros.aroscore.entity.Question;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.Topic;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.mapper.QuestionMapper;
import vn.edu.aros.aroscore.repository.QuestionRepository;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.repository.TopicRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.QuestionService;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired private QuestionRepository questionRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestionMapper questionMapper;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request) {
        String email = getCurrentUserEmail();
        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giảng viên!"));

        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        Topic topic = resolveTopic(request.getTopicId(), subject, email);

        // ==== VALIDATE LOGIC LOẠI CÂU HỎI ====
        long correctCount = request.getOptions().stream()
                .filter(AnswerOptionRequest::getIsCorrect)
                .count();

        if (request.getType() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new RuntimeException("Câu hỏi chọn 1 đáp án bắt buộc phải có CHÍNH XÁC 1 đáp án đúng!");
        }

        if (request.getType() == QuestionType.MULTIPLE_CHOICE && correctCount < 1) {
            throw new RuntimeException("Câu hỏi chọn nhiều đáp án phải có ít nhất 1 đáp án đúng!");
        }
        // =====================================

        Question question = questionMapper.toEntity(request);
        question.setSubject(subject);
        question.setTopic(topic);
        question.setTeacher(teacher);
        question.setIsActive(true);

        // Xử lý list đáp án (quan hệ 2 chiều)
        for (AnswerOptionRequest optReq : request.getOptions()) {
            AnswerOption option = AnswerOption.builder()
                    .content(optReq.getContent())
                    .isCorrect(optReq.getIsCorrect())
                    .build();
            question.addOption(option);
        }

        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Override
    public Page<QuestionResponse> getAllQuestions(Long subjectId, Long topicId, int page, int size) {
        String email = getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size);

        if (topicId != null) {
            topicRepository.findActiveByIdAndLecturerEmail(topicId, email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề hoặc bạn không có quyền!"));
            return questionRepository.findAllByTopicIdAndTeacherEmail(topicId, email, pageable)
                    .map(questionMapper::toResponse);
        }

        if (subjectId != null) {
            return questionRepository.findAllBySubjectIdAndTeacherEmail(subjectId, email, pageable)
                    .map(questionMapper::toResponse);
        }
        return questionRepository.findAllByTeacherEmail(email, pageable)
                .map(questionMapper::toResponse);
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(Long id, QuestionRequest request) {
        String email = getCurrentUserEmail();

        // 1. Tìm câu hỏi active và đảm bảo nó thuộc về giáo viên đang đăng nhập
        Question question = questionRepository.findActiveByIdAndTeacherEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi hoặc bạn không có quyền sửa!"));

        // 2. Cập nhật môn học nếu có thay đổi
        Subject subject = question.getSubject();
        if (!question.getSubject().getId().equals(request.getSubjectId())) {
            subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                    .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));
            question.setSubject(subject);
        }

        question.setTopic(resolveTopic(request.getTopicId(), subject, email));

        // 3. Validate logic loại câu hỏi
        long correctCount = request.getOptions().stream()
                .filter(AnswerOptionRequest::getIsCorrect)
                .count();

        if (request.getType() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new RuntimeException("Câu hỏi chọn 1 đáp án bắt buộc phải có CHÍNH XÁC 1 đáp án đúng!");
        }
        if (request.getType() == QuestionType.MULTIPLE_CHOICE && correctCount < 1) {
            throw new RuntimeException("Câu hỏi chọn nhiều đáp án phải có ít nhất 1 đáp án đúng!");
        }

        // 4. Cập nhật thông tin cơ bản
        question.setContent(request.getContent());
        question.setDifficulty(request.getDifficulty());
        question.setExplanation(request.getExplanation());
        question.setType(request.getType() != null ? request.getType() : QuestionType.SINGLE_CHOICE);

        // 5. Xử lý danh sách đáp án (Kỹ thuật Clear & AddAll)
        question.getOptions().clear(); // OrphanRemoval sẽ tự động xóa các record cũ dưới DB
        for (AnswerOptionRequest optReq : request.getOptions()) {
            AnswerOption option = AnswerOption.builder()
                    .content(optReq.getContent())
                    .isCorrect(optReq.getIsCorrect())
                    .build();
            question.addOption(option);
        }

        return questionMapper.toResponse(questionRepository.save(question));
    }

    /**
     * topicId null → bỏ gán chủ đề.
     * Nếu có topicId thì phải thuộc đúng subject và thuộc quyền GV.
     */
    private Topic resolveTopic(Long topicId, Subject subject, String email) {
        if (topicId == null) {
            return null;
        }
        Topic topic = topicRepository.findActiveByIdAndLecturerEmail(topicId, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề hoặc bạn không có quyền!"));
        if (!topic.getSubject().getId().equals(subject.getId())) {
            throw new RuntimeException("Chủ đề không thuộc môn học đã chọn!");
        }
        return topic;
    }

    @Override
    @Transactional
    public void softDeleteQuestion(Long id) {
        String email = getCurrentUserEmail();
        Question question = questionRepository.findByIdAndTeacherEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi hoặc bạn không có quyền xóa!"));

        if (Boolean.FALSE.equals(question.getIsActive())) {
            throw new RuntimeException("Câu hỏi đã được xóa khỏi ngân hàng!");
        }

        question.setIsActive(false);
        questionRepository.save(question);
    }
}