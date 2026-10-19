package vn.edu.aros.aroscore.service.exam;

import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.OnlineExamSettings;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Máy trạng thái đề ONLINE theo lịch.
 *
 * <pre>
 * DRAFT ──GV mở──► UPCOMING ──startAt──► ONGOING ──endAt / GV đóng──► CLOSED
 *                      │                     │
 *                      └──── GV hủy ─────────┴──► CLOSED
 * ONGOING / CLOSED ──GV kết thúc──► COMPLETED
 * </pre>
 *
 * Auto chỉ áp cho {@code UPCOMING}/{@code ONGOING}:
 * hết {@code endAt} → {@code CLOSED}; trước {@code startAt} → {@code UPCOMING}; còn lại → {@code ONGOING}.
 * {@code DRAFT}/{@code CLOSED}/{@code COMPLETED} không tự đổi theo lịch (CLOSED không tự mở lại).
 */
public final class ExamStatusLifecycle {

    private static final Set<ExamStatus> AUTO_MANAGED = EnumSet.of(ExamStatus.UPCOMING, ExamStatus.ONGOING);

    private ExamStatusLifecycle() {
    }

    /** Trạng thái hiệu lực theo lịch (không ghi DB). */
    public static ExamStatus effectiveStatus(Exam exam, LocalDateTime now) {
        if (exam == null || exam.getExamMode() == ExamMode.OMR_PAPER) {
            return ExamStatus.DRAFT;
        }
        OnlineExamSettings settings = exam.getOnlineSettings();
        if (settings == null || settings.getStatus() == null) {
            return ExamStatus.DRAFT;
        }
        return effectiveStatus(settings.getStatus(), settings.getStartAt(), settings.getEndAt(), now);
    }

    public static ExamStatus effectiveStatus(
            ExamStatus stored, LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (stored == null) {
            return ExamStatus.DRAFT;
        }
        if (!AUTO_MANAGED.contains(stored)) {
            return stored;
        }
        LocalDateTime at = now != null ? now : LocalDateTime.now();
        if (endAt != null && at.isAfter(endAt)) {
            return ExamStatus.CLOSED;
        }
        if (startAt != null && at.isBefore(startAt)) {
            return ExamStatus.UPCOMING;
        }
        return ExamStatus.ONGOING;
    }

    /**
     * Ghi lại status nếu lịch đòi hỏi thay đổi. Trả {@code true} khi đã đổi.
     * Chỉ đụng {@code UPCOMING}/{@code ONGOING}.
     */
    public static boolean syncStoredStatus(Exam exam, LocalDateTime now) {
        if (exam == null || exam.getExamMode() == ExamMode.OMR_PAPER) {
            return false;
        }
        OnlineExamSettings settings = exam.getOnlineSettings();
        if (settings == null || settings.getStatus() == null) {
            return false;
        }
        ExamStatus stored = settings.getStatus();
        if (!AUTO_MANAGED.contains(stored)) {
            return false;
        }
        ExamStatus effective = effectiveStatus(stored, settings.getStartAt(), settings.getEndAt(), now);
        if (effective == stored) {
            return false;
        }
        settings.setStatus(effective);
        return true;
    }

    /** Kiểm tra GV có được chuyển status thủ công không. */
    public static void assertManualTransition(ExamStatus from, ExamStatus to) {
        if (to == null || from == to) {
            return;
        }
        if (from == null) {
            from = ExamStatus.DRAFT;
        }
        if (!allowedManualTargets(from).contains(to)) {
            throw new IllegalArgumentException(
                    "Không thể chuyển trạng thái đề từ " + from + " sang " + to);
        }
    }

    public static boolean isTerminal(ExamStatus status) {
        return status == ExamStatus.CLOSED || status == ExamStatus.COMPLETED;
    }

    public static boolean isOpenForTaking(ExamStatus status) {
        return status == ExamStatus.UPCOMING || status == ExamStatus.ONGOING;
    }

    private static Set<ExamStatus> allowedManualTargets(ExamStatus from) {
        return switch (from) {
            case DRAFT -> EnumSet.of(ExamStatus.UPCOMING, ExamStatus.ONGOING, ExamStatus.CLOSED);
            case UPCOMING -> EnumSet.of(ExamStatus.DRAFT, ExamStatus.ONGOING, ExamStatus.CLOSED);
            case ONGOING -> EnumSet.of(ExamStatus.UPCOMING, ExamStatus.CLOSED, ExamStatus.COMPLETED);
            case CLOSED -> EnumSet.of(ExamStatus.UPCOMING, ExamStatus.ONGOING, ExamStatus.COMPLETED);
            case COMPLETED -> EnumSet.of(ExamStatus.CLOSED);
        };
    }
}
