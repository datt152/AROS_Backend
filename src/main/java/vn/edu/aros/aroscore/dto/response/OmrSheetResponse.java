package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.aros.aroscore.entity.enums.OmrSheetStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmrSheetResponse {
    private Long submissionId;
    private Long examSessionId;
    private Long examId;
    private OmrSheetStatus status;
    private String studentId;
    private Long matchedStudentId;
    private String studentName;
    private String examCode;
    private Double score;
    private Double maxScore;
    private String warpedUrl;
    private String originalImageUrl;
    private List<Integer> needReview;
    private List<OmrAnswerItemResponse> answers;
    private LocalDateTime gradedAt;
}
