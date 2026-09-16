package vn.edu.aros.aroscore.service.exam;

import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.OnlineExamSettings;
import vn.edu.aros.aroscore.entity.PaperExamSettings;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;

import java.time.LocalDateTime;

/** Đọc cấu hình Online / Paper theo mode. */
public final class ExamSettingsSupport {

    private ExamSettingsSupport() {
    }

    public static ExamStatus onlineStatus(Exam exam) {
        OnlineExamSettings s = exam.getOnlineSettings();
        return s != null && s.getStatus() != null ? s.getStatus() : ExamStatus.DRAFT;
    }

    public static boolean isTimeLimitEnabled(Exam exam) {
        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            return false;
        }
        OnlineExamSettings s = exam.getOnlineSettings();
        if (s == null || s.getTimeLimitEnabled() == null) {
            return exam.getPurpose() != ExamPurpose.PRACTICE;
        }
        return Boolean.TRUE.equals(s.getTimeLimitEnabled());
    }

    public static Integer effectiveMaxAttempts(Exam exam) {
        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            return 1;
        }
        if (exam.getPurpose() != ExamPurpose.PRACTICE) {
            return 1;
        }
        OnlineExamSettings s = exam.getOnlineSettings();
        return s != null ? s.getMaxAttempts() : null;
    }

    public static boolean showScoreToStudent(Exam exam) {
        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            return false;
        }
        OnlineExamSettings s = exam.getOnlineSettings();
        return s == null || !Boolean.FALSE.equals(s.getShowScoreToStudent());
    }

    public static boolean allowEdit(Exam exam) {
        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            return true;
        }
        OnlineExamSettings s = exam.getOnlineSettings();
        return s == null || !Boolean.FALSE.equals(s.getAllowEdit());
    }

    public static boolean shuffleQuestions(Exam exam) {
        PaperExamSettings p = exam.getPaperSettings();
        return p == null || !Boolean.FALSE.equals(p.getShuffleQuestions());
    }

    public static boolean shuffleAnswers(Exam exam) {
        PaperExamSettings p = exam.getPaperSettings();
        return p == null || !Boolean.FALSE.equals(p.getShuffleAnswers());
    }

    public static int paperCount(Exam exam) {
        PaperExamSettings p = exam.getPaperSettings();
        if (p == null || p.getPaperCount() == null || p.getPaperCount() < 1) {
            return 1;
        }
        return p.getPaperCount();
    }

    public static LocalDateTime startAt(Exam exam) {
        OnlineExamSettings s = exam.getOnlineSettings();
        return s != null ? s.getStartAt() : null;
    }

    public static LocalDateTime endAt(Exam exam) {
        OnlineExamSettings s = exam.getOnlineSettings();
        return s != null ? s.getEndAt() : null;
    }
}
