package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.request.BulkCreateAccountsRequest;
import vn.edu.aros.aroscore.dto.response.BulkCreateAccountsResultResponse;

public interface BulkAccountService {

    BulkCreateAccountsResultResponse createAccountsForClassroom(Long classId, BulkCreateAccountsRequest request);
}
