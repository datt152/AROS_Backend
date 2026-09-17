package vn.edu.aros.aroscore.dto.print;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExamPaperPrintModel {
    private String orgLine1;
    private String orgLine2;
    private String orgLine3;

    private String title;
    private String subjectName;
    private String className;
    private String academicYearLabel;
    private String examDate;
    private Integer durationMinutes;
    private String versionCode;

    private List<QuestionBlock> questions;

    @Data
    @Builder
    public static class QuestionBlock {
        private int order;
        private String content;
        private List<OptionBlock> options;
    }

    @Data
    @Builder
    public static class OptionBlock {
        private String label;
        private String content;
    }
}
