package vn.edu.aros.aroscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportResultResponse {
    private int total;
    private int success;
    private int failed;
    private int skipped;

    @Builder.Default
    private List<StudentImportRowResult> errors = new ArrayList<>();

    @Builder.Default
    private List<StudentImportRowResult> successes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentImportRowResult {
        private int row;
        private String email;
        private String fullName;
        private String studentCode;
        private String message;
    }
}
