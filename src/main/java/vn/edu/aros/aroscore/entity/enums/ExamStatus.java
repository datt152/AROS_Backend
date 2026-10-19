package vn.edu.aros.aroscore.entity.enums;

/**
 * Trạng thái đề ONLINE ({@code online_exam_settings.status}).
 *
 * <ul>
 *   <li>{@link #DRAFT} — đang soạn, SV không thấy / không làm</li>
 *   <li>{@link #UPCOMING} — đã mở, chưa tới {@code startAt}</li>
 *   <li>{@link #ONGOING} — trong cửa sổ làm bài</li>
 *   <li>{@link #CLOSED} — hết hạn ({@code endAt}) hoặc GV đóng; không làm / nộp</li>
 *   <li>{@link #COMPLETED} — GV đánh dấu kết thúc nghiệp vụ (sau CLOSED hoặc từ ONGOING)</li>
 * </ul>
 *
 * {@code UPCOMING}/{@code ONGOING} được đồng bộ tự động theo lịch; {@code CLOSED} không tự mở lại.
 */
public enum ExamStatus {
    DRAFT,
    UPCOMING,
    ONGOING,
    CLOSED,
    COMPLETED
}
