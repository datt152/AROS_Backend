package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.Question;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.mapper.ExamMapper;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.QuestionRepository;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.ExamService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamMapper examMapper;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public ExamResponse createExam(ExamCreateRequest request) {
        String email = getCurrentUserEmail();
        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giáo viên!"));

        // 1. Kiểm tra môn học
        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền truy cập!"));

        // 2. Lấy danh sách câu hỏi từ Database
        List<Question> questions = questionRepository.findAllById(request.getQuestionIds());
        if (questions.size() != request.getQuestionIds().size()) {
            throw new RuntimeException("Một số câu hỏi không tồn tại trong hệ thống!");
        }

        // 3. Kiểm tra loại câu hỏi
        for (Question q : questions) {
            // Check 3.1: Câu hỏi có thuộc đúng môn học này không?
            if (!q.getSubject().getId().equals(subject.getId())) {
                throw new RuntimeException("Câu hỏi ID " + q.getId() + " không thuộc môn học này!");
            }

            // Check 3.2: Đề OMR cấm câu hỏi MULTIPLE_CHOICE
            if (request.getExamMode() == ExamMode.OMR_PAPER && q.getType() == QuestionType.MULTIPLE_CHOICE) {
                throw new RuntimeException("LỖI: Đề thi OMR không được chứa câu hỏi nhiều đáp án (ID: " + q.getId() + ")");
            }
        }

        // 4. Khởi tạo Đề thi gốc
        Exam exam = Exam.builder()
                .title(request.getTitle())
                .duration(request.getDuration())
                .examMode(request.getExamMode())
                .subject(subject)
                .teacher(teacher)
                .build();

        // 5. Gắp câu hỏi vào đề
        int order = 1;
        for (Long questionId : request.getQuestionIds()) {
            Question matchedQuestion = questions.stream()
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElseThrow();

            exam.addQuestion(matchedQuestion, order);
            order++;
        }

        // 6. Lưu xuống DB
        Exam savedExam = examRepository.save(exam);

        return examMapper.toResponse(savedExam);
    }
}