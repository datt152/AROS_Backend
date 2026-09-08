package vn.edu.aros.aroscore.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.response.StudentImportResultResponse;

public interface StudentImportService {

    StudentImportResultResponse importStudentsFromExcel(Long classId, MultipartFile file);
}
