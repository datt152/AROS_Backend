package vn.edu.aros.aroscore.service;

import org.springframework.data.domain.Page;
import vn.edu.aros.aroscore.dto.request.TopicRequest;
import vn.edu.aros.aroscore.dto.response.TopicResponse;

public interface TopicService {
    TopicResponse createTopic(TopicRequest request);

    Page<TopicResponse> getTopicsBySubject(Long subjectId, int page, int size);

    TopicResponse getTopicById(Long id);

    TopicResponse updateTopic(Long id, TopicRequest request);

    void softDeleteTopic(Long id);
}
