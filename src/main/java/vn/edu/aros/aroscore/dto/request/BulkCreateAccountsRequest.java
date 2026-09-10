package vn.edu.aros.aroscore.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkCreateAccountsRequest {

    private List<Long> studentIds;
}
