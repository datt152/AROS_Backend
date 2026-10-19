package vn.edu.aros.aroscore.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.aros.aroscore.service.ExamStatusSyncService;

/**
 * Định kỳ đẩy status đề ONLINE theo lịch (UPCOMING→ONGOING, *→CLOSED khi hết endAt).
 */
@Component
@RequiredArgsConstructor
public class ExamStatusSyncScheduler {

    private final ExamStatusSyncService examStatusSyncService;

    @Scheduled(fixedDelayString = "${app.exam-status-sync.fixed-delay-ms:60000}")
    public void syncDueExamStatuses() {
        examStatusSyncService.syncDueExams();
    }
}
