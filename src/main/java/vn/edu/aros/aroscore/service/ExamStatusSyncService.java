package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.entity.Exam;

/** Đồng bộ {@code online_exam_settings.status} theo lịch (persist). */
public interface ExamStatusSyncService {

    /** Sync một đề; trả true nếu status đã đổi và đã lưu. */
    boolean syncExam(Exam exam);

    /** Đóng / chuyển phase mọi đề UPCOMING|ONGOING cần cập nhật theo lịch. */
    int syncDueExams();
}
