package vn.edu.aros.aroscore.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.aros.aroscore.dto.response.QuestionImportResult;

public interface QuestionImportService {
    /**
     * Import câu hỏi từ file Excel vào một Topic cụ thể
     * @param file File Excel (.xlsx, .xls)
     * @param topicId ID của Topic
     * @param teacherId ID của giáo viên (người tạo câu hỏi)
     * @return Kết quả import (số câu thành công, lỗi chi tiết từng dòng)
     */
    QuestionImportResult importQuestionsFromExcel(
        MultipartFile file,
        Long topicId,
        Long teacherId
    );
}
