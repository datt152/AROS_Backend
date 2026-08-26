package vn.edu.aros.aroscore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.request.AssignExamClassroomsRequest;
import vn.edu.aros.aroscore.dto.request.ExamCreateRequest;
import vn.edu.aros.aroscore.dto.request.ExamUpdateRequest;
import vn.edu.aros.aroscore.dto.request.ExamVersionCreateRequest;
import vn.edu.aros.aroscore.dto.response.ClassroomResponse;
import vn.edu.aros.aroscore.dto.response.ExamGradingResponse;
import vn.edu.aros.aroscore.dto.response.ExamResponse;
import vn.edu.aros.aroscore.dto.response.ExamStatsResponse;
import vn.edu.aros.aroscore.dto.response.ExamTakeResponse;
import vn.edu.aros.aroscore.dto.response.ExamVersionDetailResponse;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;

import java.util.List;

public interface ExamService {
    ExamResponse createExam(ExamCreateRequest request);

    List<String> generateExamVersions(ExamVersionCreateRequest request);

    ExamVersionDetailResponse getExamVersionDetail(Long examId, String versionCode) throws JsonProcessingException;

    Page<ExamResponse> getAllExams(Long classroomId, ExamPurpose purpose, Pageable pageable);

    ExamResponse getExamById(Long id);

    @Transactional
    ExamResponse updateExam(Long id, ExamUpdateRequest request);

    @Transactional
    void deleteExam(Long id);

    ExamTakeResponse takeExam(Long examId);

    @Transactional
    ExamResponse assignClassrooms(Long examId, AssignExamClassroomsRequest request);

    List<ClassroomResponse> getAssignedClassrooms(Long examId);

    List<String> listVersionCodes(Long examId);

    ExamGradingResponse getExamGrading(Long examId, Long classroomId);

    ExamStatsResponse getExamStats(Long examId, Long classroomId);
}
