package vn.edu.aros.aroscore.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.request.OmrReviewRequest;
import vn.edu.aros.aroscore.dto.response.OmrSheetResponse;

import java.util.List;

public interface OmrSheetService {

    OmrSheetResponse uploadAndScan(Long sessionId, MultipartFile file);

    OmrSheetResponse getSheet(Long sheetId);

    List<OmrSheetResponse> listBySession(Long sessionId);

    OmrSheetResponse reviewAndRegrade(Long sheetId, OmrReviewRequest request);
}
