package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.TopicRequest;
import vn.edu.aros.aroscore.dto.response.TopicResponse;
import vn.edu.aros.aroscore.entity.Subject;
import vn.edu.aros.aroscore.entity.Topic;
import vn.edu.aros.aroscore.repository.QuestionRepository;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.repository.TopicRepository;
import vn.edu.aros.aroscore.service.TopicService;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional
    public TopicResponse createTopic(TopicRequest request) {
        String email = getCurrentUserEmail();
        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        if (topicRepository.existsByNameAndSubjectId(request.getName().trim(), subject.getId())) {
            throw new RuntimeException("Chủ đề \"" + request.getName() + "\" đã tồn tại trong môn này!");
        }

        Topic topic = Topic.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .subject(subject)
                .isActive(true)
                .build();

        return toResponse(topicRepository.save(topic));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TopicResponse> getTopicsBySubject(Long subjectId, int page, int size) {
        String email = getCurrentUserEmail();
        if (!subjectRepository.findByIdAndLecturerEmail(subjectId, email).isPresent()) {
            throw new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()));
        return topicRepository.findAllBySubjectIdAndLecturerEmail(subjectId, email, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TopicResponse getTopicById(Long id) {
        Topic topic = topicRepository.findByIdAndLecturerEmail(id, getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề hoặc bạn không có quyền!"));
        return toResponse(topic);
    }

    @Override
    @Transactional
    public TopicResponse updateTopic(Long id, TopicRequest request) {
        String email = getCurrentUserEmail();
        Topic topic = topicRepository.findActiveByIdAndLecturerEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề hoặc bạn không có quyền!"));

        Subject subject = subjectRepository.findByIdAndLecturerEmail(request.getSubjectId(), email)
                .orElseThrow(() -> new RuntimeException("Môn học không tồn tại hoặc bạn không có quyền!"));

        String newName = request.getName().trim();
        if (topicRepository.existsByNameAndSubjectIdAndIdNot(newName, subject.getId(), topic.getId())) {
            throw new RuntimeException("Chủ đề \"" + newName + "\" đã tồn tại trong môn này!");
        }

        topic.setName(newName);
        topic.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) {
            topic.setDisplayOrder(request.getDisplayOrder());
        }
        topic.setSubject(subject);

        return toResponse(topicRepository.save(topic));
    }

    @Override
    @Transactional
    public void softDeleteTopic(Long id) {
        String email = getCurrentUserEmail();
        Topic topic = topicRepository.findActiveByIdAndLecturerEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ đề hoặc bạn không có quyền!"));

        long activeQuestions = questionRepository.countActiveByTopicId(topic.getId());
        if (activeQuestions > 0) {
            throw new RuntimeException(
                    "Không thể xóa chủ đề đang chứa " + activeQuestions + " câu hỏi active. Hãy chuyển hoặc xóa câu hỏi trước!");
        }

        topic.setIsActive(false);
        topicRepository.save(topic);
    }

    private TopicResponse toResponse(Topic topic) {
        long count = questionRepository.countActiveByTopicId(topic.getId());
        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .displayOrder(topic.getDisplayOrder())
                .subjectId(topic.getSubject().getId())
                .subjectName(topic.getSubject().getSubjectName())
                .isActive(topic.getIsActive())
                .questionCount(count)
                .build();
    }
}
