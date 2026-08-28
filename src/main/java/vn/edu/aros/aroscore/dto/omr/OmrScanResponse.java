package vn.edu.aros.aroscore.dto.omr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OmrScanResponse {

    private Boolean success;
    private String error;
    private String message;
    private String hint;
    @JsonProperty("template_version")
    private String templateVersion;
    private OmrCanvas canvas;
    private OmrImages images;
    @JsonProperty("student_id")
    private OmrCodeField studentId;
    @JsonProperty("exam_code")
    private OmrCodeField examCode;
    private OmrAnswersBlock answers;
    private OmrSummary summary;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrCanvas {
        private Integer width;
        private Integer height;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrImages {
        @JsonProperty("warped_url")
        private String warpedUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrCodeField {
        private String value;
        private Boolean valid;
        private List<String> warnings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrAnswersBlock {
        private Map<String, OmrAnswerItem> items;
        @JsonProperty("need_review")
        private List<Integer> needReview;
        @JsonProperty("total_questions")
        private Integer totalQuestions;
        @JsonProperty("answered_count")
        private Integer answeredCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrAnswerItem {
        private String chosen;
        private String status;
        private Integer column;
        private OmrBubble bubble;
        private List<OmrBubble> bubbles;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrBubble {
        private String choice;
        private Integer x;
        private Integer y;
        private Integer w;
        private Integer h;
        private Double score;
        private Boolean selected;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OmrSummary {
        @JsonProperty("student_id")
        private String studentId;
        @JsonProperty("exam_code")
        private String examCode;
        @JsonProperty("student_id_valid")
        private Boolean studentIdValid;
        @JsonProperty("exam_code_valid")
        private Boolean examCodeValid;
        @JsonProperty("questions_need_review")
        private List<Integer> questionsNeedReview;
        @JsonProperty("review_count")
        private Integer reviewCount;
        @JsonProperty("answered_count")
        private Integer answeredCount;
    }
}
