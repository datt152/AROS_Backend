package vn.edu.aros.aroscore.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.matrix.AnswerMapping;
import vn.edu.aros.aroscore.dto.matrix.QuestionMatrix;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamUpdateRequest;
import vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamVersionDetailResponse;
import vn.edu.aros.aroscore.dto.response.OptionInVersionResponse;
import vn.edu.aros.aroscore.dto.response.QuestionInVersionResponse;
import vn.edu.aros.aroscore.entity.*;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.QuestionType;
import vn.edu.aros.aroscore.mapper.ExamMapper;
import vn.edu.aros.aroscore.repository.*;
import vn.edu.aros.aroscore.service.ExamService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamMapper examMapper;
    private final ExamVersionRepository examVersionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .maxScore(request.getMaxScore()) // Lưu thang điểm chuẩn
                .build();

        // 5. Gắp câu hỏi vào đề và gán điểm thô (trọng số)
        int order = 1;
        for (Long questionId : request.getQuestionIds()) {
            Question matchedQuestion = questions.stream()
                    .filter(q -> q.getId().equals(questionId))
                    .findFirst()
                    .orElseThrow();

            // Xử lý điểm thô: Mặc định là 1.0. Nếu có gán tay thì lấy giá trị gán tay.
            Double rawPoint = 1.0;
            if (request.getRawPoints() != null && request.getRawPoints().containsKey(questionId)) {
                rawPoint = request.getRawPoints().get(questionId);
            }

            exam.addQuestion(matchedQuestion, order, rawPoint);
            order++;
        }

        // 6. Lưu xuống DB
        Exam savedExam = examRepository.save(exam);

        return examMapper.toResponse(savedExam);
    }
    @Override
    @Transactional
    public List<String> generateExamVersions(ExamVersionCreateRequest request) {
        // 1. Lấy đề gốc
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Đề thi gốc!"));

        // 2. Quyết định danh sách mã đề (Tự nhập hay Tự sinh)
        List<String> finalCodes = new java.util.ArrayList<>();

        if (request.getManualVersionCodes() != null && !request.getManualVersionCodes().isEmpty()) {
            finalCodes.addAll(request.getManualVersionCodes());
        } else if (request.getAutoGenerateCount() != null && request.getAutoGenerateCount() > 0) {
            // Tự sinh mã từ 001, 002...
            for (int i = 1; i <= request.getAutoGenerateCount(); i++) {
                finalCodes.add(String.format("%03d", i));
            }
        } else {
            throw new RuntimeException("Bạn phải cung cấp danh sách mã đề hoặc số lượng đề cần tự sinh!");
        }

        // 3. THUẬT TOÁN TRỘN ĐỀ
        String[] LABELS = {"A", "B", "C", "D", "E", "F", "G", "H"}; // Hỗ trợ lên tới 8 đáp án
        List<ExamVersion> savedVersions = new java.util.ArrayList<>();

        for (String code : finalCodes) {
            // 3.1 Clone danh sách câu hỏi gốc ra một list mới và Xáo trộn
            List<vn.edu.aros.aroscore.entity.ExamQuestion> shuffledQuestions = new java.util.ArrayList<>(exam.getExamQuestions());
            java.util.Collections.shuffle(shuffledQuestions);

            List<vn.edu.aros.aroscore.dto.matrix.QuestionMatrix> matrixList = new java.util.ArrayList<>();
            int newQuestionOrder = 1;

            for (vn.edu.aros.aroscore.entity.ExamQuestion eq : shuffledQuestions) {
                vn.edu.aros.aroscore.entity.Question q = eq.getQuestion();

                // 3.2 Clone danh sách đáp án của câu hỏi này và Xáo trộn
                List<vn.edu.aros.aroscore.entity.AnswerOption> shuffledOptions = new java.util.ArrayList<>(q.getOptions());
                java.util.Collections.shuffle(shuffledOptions);

                // 3.3 Lưu vết đáp án (Map ID đáp án cũ với nhãn A, B, C, D mới)
                List<vn.edu.aros.aroscore.dto.matrix.AnswerMapping> answerMappings = new java.util.ArrayList<>();
                for (int i = 0; i < shuffledOptions.size(); i++) {
                    vn.edu.aros.aroscore.entity.AnswerOption opt = shuffledOptions.get(i);
                    answerMappings.add(new vn.edu.aros.aroscore.dto.matrix.AnswerMapping(
                            opt.getId(),
                            LABELS[i],
                            opt.getIsCorrect()
                    ));
                }

                // 3.4 Lưu vết câu hỏi
                matrixList.add(new vn.edu.aros.aroscore.dto.matrix.QuestionMatrix(
                        q.getId(),
                        newQuestionOrder,
                        answerMappings
                ));
                newQuestionOrder++;
            }

            // 4. Parse Ma trận thành JSON String và tạo ExamVersion
            try {
                String jsonMatrix = objectMapper.writeValueAsString(matrixList);

                vn.edu.aros.aroscore.entity.ExamVersion version = vn.edu.aros.aroscore.entity.ExamVersion.builder()
                        .exam(exam)
                        .versionCode(code)
                        .shuffleMatrix(jsonMatrix)
                        .build();

                savedVersions.add(version);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Lỗi hệ thống khi sinh ma trận hoán vị JSON", e);
            }
        }

        // 5. Lưu toàn bộ các mã đề xuống Database (Batch Insert)
        examVersionRepository.saveAll(savedVersions);

        return finalCodes; // Trả về danh sách mã đề đã tạo thành công
    }
    @Override
    @Transactional(readOnly = true)
    public ExamVersionDetailResponse getExamVersionDetail(Long examId, String versionCode) throws JsonProcessingException {
        // 1. Tìm mã đề
        ExamVersion version = examVersionRepository.findByExamIdAndVersionCode(examId, versionCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã đề " + versionCode + " của kỳ thi này!"));

        Exam exam = version.getExam();

        // 2. Đọc chuỗi JSON thành List<QuestionMatrix>
        List<QuestionMatrix> matrixList = objectMapper.readValue(
                version.getShuffleMatrix(),
                new TypeReference<List<QuestionMatrix>>() {}
        );

        // Đảm bảo list đã được sắp xếp theo đúng thứ tự câu 1, 2, 3...
        matrixList.sort(Comparator.comparingInt(QuestionMatrix::getNewOrder));

        List<QuestionInVersionResponse> questionResponses = new java.util.ArrayList<>();

        // 3. Vòng lặp giải mã từng câu hỏi
        for (QuestionMatrix qMatrix : matrixList) {

            // Tìm lại câu hỏi gốc từ DB
            ExamQuestion originalEq = exam.getExamQuestions().stream()
                    .filter(eq -> eq.getQuestion().getId().equals(qMatrix.getOriginalQuestionId()))
                    .findFirst()
                    .orElseThrow();

            Question originalQ = originalEq.getQuestion();

            // Ánh xạ lại danh sách đáp án
            List<OptionInVersionResponse> optionResponses = new java.util.ArrayList<>();
            for (AnswerMapping aMap : qMatrix.getAnswerMappings()) {
                AnswerOption originalOpt = originalQ.getOptions().stream()
                        .filter(opt -> opt.getId().equals(aMap.getOriginalOptionId()))
                        .findFirst()
                        .orElseThrow();

                OptionInVersionResponse optRes = new OptionInVersionResponse();
                optRes.setLabel(aMap.getNewLabel()); // Gắn nhãn A, B, C, D
                optRes.setContent(originalOpt.getContent());
                optionResponses.add(optRes);
            }

            // Sắp xếp các đáp án theo vần A, B, C, D
            optionResponses.sort(Comparator.comparing(OptionInVersionResponse::getLabel));

            // Đóng gói câu hỏi
            QuestionInVersionResponse qRes = new QuestionInVersionResponse();
            qRes.setOriginalQuestionId(originalQ.getId());
            qRes.setContent(originalQ.getContent());
            qRes.setType(originalQ.getType());
            qRes.setOptions(optionResponses);

            questionResponses.add(qRes);
        }

        // 4. Trả về kết quả tổng
        ExamVersionDetailResponse response = new ExamVersionDetailResponse();
        response.setExamId(exam.getId());
        response.setTitle(exam.getTitle());
        response.setDuration(exam.getDuration());
        response.setVersionCode(version.getVersionCode());
        response.setQuestions(questionResponses);

        return response;
    }
    @Override
    public Page<Exam> getAllExams(Pageable pageable) {
        // Lấy thông tin user đang đăng nhập
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

        // Kiểm tra xem User này có phải ADMIN không
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // Admin thì được xem tất cả
            return examRepository.findAll(pageable);
        } else {
            // Giáo viên thì chỉ xem đề thi của chính mình
            return examRepository.findAllByTeacherEmail(currentUser.getEmail(), pageable);
        }
    }

    @Override
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi với ID: " + id));
    }

    @Transactional
    @Override
    public Exam updateExam(Long id, ExamUpdateRequest request) {
        Exam exam = getExamById(id);

        // Cập nhật thông tin cơ bản
        exam.setTitle(request.getTitle());
        exam.setDuration(request.getDuration());
        exam.setExamMode(request.getExamMode());
        exam.setMaxScore(request.getMaxScore());

        // (Tùy chọn) Cập nhật Subject nếu có thay đổi
        if (!exam.getSubject().getId().equals(request.getSubjectId())) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học"));
            exam.setSubject(subject);
        }
        return examRepository.save(exam);
    }

    @Transactional
    @Override
    public void deleteExam(Long id) {
        Exam exam = getExamById(id);
        // Kiểm tra xem đề thi đã có học sinh nộp bài chưa
        // Nếu có rồi thì không cho xóa để bảo toàn dữ liệu điểm số
        examRepository.delete(exam);
    }
}