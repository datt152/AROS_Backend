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
public class BulkCreateAccountsResultResponse {

    private int total;
    private int created;
    private int skipped;
    private int failed;
    /** Số mail đã xếp hàng gửi (async) */
    private int mailQueued;

    @Builder.Default
    private List<Item> results = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long studentId;
        private String email;
        private String fullName;
        /** CREATED | SKIPPED | FAILED */
        private String status;
        private String message;
    }
}
