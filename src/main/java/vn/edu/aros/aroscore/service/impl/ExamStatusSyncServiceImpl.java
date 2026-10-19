package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.service.ExamStatusSyncService;
import vn.edu.aros.aroscore.service.exam.ExamStatusLifecycle;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamStatusSyncServiceImpl implements ExamStatusSyncService {

    private final ExamRepository examRepository;

    @Override
    @Transactional
    public boolean syncExam(Exam exam) {
        if (exam == null) {
            return false;
        }
        boolean changed = ExamStatusLifecycle.syncStoredStatus(exam, LocalDateTime.now());
        if (changed) {
            examRepository.save(exam);
        }
        return changed;
    }

    @Override
    @Transactional
    public int syncDueExams() {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> candidates = examRepository.findOnlineExamsNeedingStatusSync(now);
        int updated = 0;
        for (Exam exam : candidates) {
            ExamStatus before = exam.getOnlineSettings() != null
                    ? exam.getOnlineSettings().getStatus()
                    : null;
            if (ExamStatusLifecycle.syncStoredStatus(exam, now)) {
                examRepository.save(exam);
                updated++;
                log.debug("Synced exam {} status {} -> {}",
                        exam.getId(), before,
                        exam.getOnlineSettings() != null ? exam.getOnlineSettings().getStatus() : null);
            }
        }
        if (updated > 0) {
            log.info("Exam status sync: updated {} exam(s)", updated);
        }
        return updated;
    }
}
