package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStatsResponse {
    private Long examId;
    private String examTitle;
    private Double maxScore;
    private Long classroomId;
    private String classroomName;

    private Integer totalStudents;
    private Integer submittedCount;
    private Integer inProgressCount;
    private Integer expiredCount;
    private Integer notStartedCount;

    private Double averageScore;
    private Double highestScore;
    private Double lowestScore;

    private List<ScoreBucketResponse> scoreDistribution;
    private List<QuestionStatsResponse> questionStats;
}
