package vn.edu.aros.aroscore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionResponse {
    private Long submissionId;
    private Double totalScore;        // Điểm thực tế đạt được
    private Double maxScore;          // Thang điểm chuẩn
    private Integer correctQuestions; // Số câu đúng
    private Integer totalQuestions;   // Tổng số câu
}