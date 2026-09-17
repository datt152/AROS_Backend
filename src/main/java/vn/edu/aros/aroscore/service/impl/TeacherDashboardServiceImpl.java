package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.response.dashboard.TeacherDashboardCalendarEventResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.TeacherDashboardResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.TeacherDashboardStatsResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.TeacherDashboardTodoResponse;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.OMRFile;
import vn.edu.aros.aroscore.entity.OnlineExamSettings;
import vn.edu.aros.aroscore.entity.PaperExamSettings;
import vn.edu.aros.aroscore.entity.enums.ExamMode;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamSessionStatus;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.OmrSheetStatus;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.ExamSessionRepository;
import vn.edu.aros.aroscore.repository.ExamTemplateRepository;
import vn.edu.aros.aroscore.repository.ExamVersionRepository;
import vn.edu.aros.aroscore.repository.OMRFileRepository;
import vn.edu.aros.aroscore.repository.QuestionRepository;
import vn.edu.aros.aroscore.repository.SubjectRepository;
import vn.edu.aros.aroscore.service.TeacherDashboardService;
import vn.edu.aros.aroscore.service.exam.ExamSettingsSupport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int ENDING_SOON_HOURS = 2;
    private static final int MAX_TODOS = 20;
    private static final int MAX_OMR_REVIEW_TODOS = 5;

    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final QuestionRepository questionRepository;
    private final ExamTemplateRepository examTemplateRepository;
    private final ExamVersionRepository examVersionRepository;
    private final OMRFileRepository omrFileRepository;

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardResponse getDashboard(LocalDate from, LocalDate to) {
        String email = currentEmail();
        LocalDate today = LocalDate.now(VN);
        LocalDate rangeFrom = from != null ? from : today.minusMonths(1).withDayOfMonth(1);
        LocalDate rangeTo = to != null ? to : today.plusMonths(1).withDayOfMonth(today.plusMonths(1).lengthOfMonth());
        if (rangeTo.isBefore(rangeFrom)) {
            throw new IllegalArgumentException("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu!");
        }

        List<Exam> exams = examRepository.findAllByTeacherEmailWithSettings(email);
        List<TeacherDashboardTodoResponse> todos = buildTodos(email, exams);
        TeacherDashboardStatsResponse stats = buildStats(email, todos.size());
        List<TeacherDashboardCalendarEventResponse> events = buildCalendarEvents(exams, rangeFrom, rangeTo);

        return TeacherDashboardResponse.builder()
                .generatedAt(Instant.now())
                .stats(stats)
                .todos(todos)
                .activities(List.of())
                .calendarEvents(events)
                .build();
    }

    private TeacherDashboardStatsResponse buildStats(String email, int pendingActionCount) {
        long onlineExamCount = examRepository.countByTeacherEmailAndModeAndPurpose(
                email, ExamMode.ONLINE, ExamPurpose.EXAM);
        long onlineOngoing = examRepository.countByTeacherEmailAndModePurposeAndOnlineStatus(
                email, ExamMode.ONLINE, ExamPurpose.EXAM, ExamStatus.ONGOING);
        long onlineUpcoming = examRepository.countByTeacherEmailAndModePurposeAndOnlineStatus(
                email, ExamMode.ONLINE, ExamPurpose.EXAM, ExamStatus.UPCOMING);
        long omrExamCount = examRepository.countByTeacherEmailAndModeAndPurpose(
                email, ExamMode.OMR_PAPER, ExamPurpose.EXAM);
        long practiceCount = examRepository.countByTeacherEmailAndModeAndPurpose(
                email, ExamMode.ONLINE, ExamPurpose.PRACTICE)
                + examRepository.countByTeacherEmailAndModeAndPurpose(
                email, ExamMode.OMR_PAPER, ExamPurpose.PRACTICE);
        long practiceOpen = examRepository.countByTeacherEmailAndModePurposeAndOnlineStatus(
                email, ExamMode.ONLINE, ExamPurpose.PRACTICE, ExamStatus.ONGOING)
                + examRepository.countByTeacherEmailAndModePurposeAndOnlineStatus(
                email, ExamMode.ONLINE, ExamPurpose.PRACTICE, ExamStatus.UPCOMING);

        return TeacherDashboardStatsResponse.builder()
                .classroomCount(classroomRepository.countActiveByLecturerEmail(email))
                .subjectCount(subjectRepository.countActiveByLecturerEmail(email))
                .onlineExamCount(onlineExamCount)
                .onlineExamOngoingCount(onlineOngoing)
                .onlineExamUpcomingCount(onlineUpcoming)
                .omrExamCount(omrExamCount)
                .omrActiveSessionCount(examSessionRepository.countOmrByTeacherEmailAndStatus(
                        email, ExamSessionStatus.OPEN))
                .practiceCount(practiceCount)
                .practiceOpenCount(practiceOpen)
                .questionCount(questionRepository.countActiveByTeacherEmail(email))
                .questionAddedLast7Days(0L)
                .templateCount(examTemplateRepository.countActiveByTeacherEmail(email))
                .pendingActionCount(pendingActionCount)
                .build();
    }

    private List<TeacherDashboardTodoResponse> buildTodos(String email, List<Exam> exams) {
        List<TeacherDashboardTodoResponse> todos = new ArrayList<>();

        for (Exam exam : exams) {
            if (exam.getPurpose() != ExamPurpose.EXAM) {
                continue;
            }
            boolean hasClass = exam.getClassrooms() != null && !exam.getClassrooms().isEmpty();
            boolean hasQuestions = exam.getExamQuestions() != null && !exam.getExamQuestions().isEmpty();
            boolean hasVersions = examVersionRepository.existsByExamId(exam.getId());

            if (exam.getExamMode() == ExamMode.ONLINE) {
                ExamStatus status = ExamSettingsSupport.onlineStatus(exam);
                if (status == ExamStatus.DRAFT && (!hasClass || !hasQuestions)) {
                    todos.add(TeacherDashboardTodoResponse.builder()
                            .id("todo-draft-" + exam.getId())
                            .type("DRAFT_INCOMPLETE")
                            .priority("MEDIUM")
                            .title("Hoàn thiện đề nháp: " + exam.getTitle())
                            .detail(buildDraftDetail(!hasClass, !hasQuestions))
                            .entityType("EXAM")
                            .entityId(exam.getId())
                            .actionPath("/teacher/exams/online/" + exam.getId())
                            .build());
                }
            }

            if (hasClass && !hasVersions) {
                String path = exam.getExamMode() == ExamMode.OMR_PAPER
                        ? "/teacher/exams/omr/" + exam.getId()
                        : "/teacher/exams/online/" + exam.getId();
                todos.add(TeacherDashboardTodoResponse.builder()
                        .id("todo-versions-" + exam.getId())
                        .type("MISSING_VERSIONS")
                        .priority("HIGH")
                        .title("Chưa sinh mã đề: " + exam.getTitle())
                        .detail("Đề đã giao lớp nhưng chưa có mã đề")
                        .entityType("EXAM")
                        .entityId(exam.getId())
                        .actionPath(path)
                        .build());
            }
        }

        List<OMRFile> reviewSheets = omrFileRepository.findByTeacherEmailAndStatus(
                email, OmrSheetStatus.NEEDS_REVIEW);
        int omrAdded = 0;
        for (OMRFile sheet : reviewSheets) {
            if (omrAdded >= MAX_OMR_REVIEW_TODOS) {
                break;
            }
            todos.add(TeacherDashboardTodoResponse.builder()
                    .id("todo-omr-review-" + sheet.getId())
                    .type("OMR_SHEETS_NEED_REVIEW")
                    .priority("HIGH")
                    .title("Phiếu OMR cần review #" + sheet.getId())
                    .detail("Có phiếu scan nghi ngờ / cần xác nhận lại.")
                    .entityType("OMR_SHEET")
                    .entityId(sheet.getId())
                    .actionPath("/teacher/omr/sheets/" + sheet.getId())
                    .build());
            omrAdded++;
        }

        todos.sort(Comparator
                .comparing((TeacherDashboardTodoResponse t) -> priorityRank(t.getPriority()))
                .thenComparing(TeacherDashboardTodoResponse::getId));

        if (todos.size() > MAX_TODOS) {
            return new ArrayList<>(todos.subList(0, MAX_TODOS));
        }
        return todos;
    }

    private static int priorityRank(String priority) {
        if ("HIGH".equals(priority)) {
            return 0;
        }
        if ("MEDIUM".equals(priority)) {
            return 1;
        }
        return 2;
    }

    private static String buildDraftDetail(boolean missingClass, boolean missingQuestions) {
        List<String> parts = new ArrayList<>();
        if (missingQuestions) {
            parts.add("thiếu câu hỏi");
        }
        if (missingClass) {
            parts.add("chưa giao lớp");
        }
        return "Đề nháp " + String.join(", ", parts) + ".";
    }

    private List<TeacherDashboardCalendarEventResponse> buildCalendarEvents(
            List<Exam> exams, LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now(VN);
        List<TeacherDashboardCalendarEventResponse> events = new ArrayList<>();

        for (Exam exam : exams) {
            TeacherDashboardCalendarEventResponse event = toCalendarEvent(exam, now);
            if (event == null) {
                continue;
            }
            if (event.getDate().isBefore(from) || event.getDate().isAfter(to)) {
                continue;
            }
            events.add(event);
        }

        events.sort(Comparator
                .comparing(TeacherDashboardCalendarEventResponse::getDate)
                .thenComparing(e -> e.getStartAt() != null ? e.getStartAt().toLocalDateTime() : LocalDateTime.MIN));
        return events;
    }

    private TeacherDashboardCalendarEventResponse toCalendarEvent(Exam exam, LocalDateTime now) {
        String kind;
        ExamStatus status;
        LocalDateTime start;
        LocalDateTime end;
        LocalDate date;

        if (exam.getPurpose() == ExamPurpose.PRACTICE) {
            kind = "PRACTICE";
        } else if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            kind = "OMR";
        } else {
            kind = "ONLINE";
        }

        if (exam.getExamMode() == ExamMode.OMR_PAPER) {
            PaperExamSettings paper = exam.getPaperSettings();
            if (paper == null || paper.getExamDate() == null) {
                return null;
            }
            date = paper.getExamDate();
            start = date.atStartOfDay();
            end = date.atTime(LocalTime.MAX).truncatedTo(ChronoUnit.SECONDS);
            status = resolveOmrPhaseStatus(date, now.toLocalDate());
        } else {
            OnlineExamSettings online = exam.getOnlineSettings();
            status = online != null && online.getStatus() != null ? online.getStatus() : ExamStatus.DRAFT;
            if (status == ExamStatus.DRAFT) {
                return null;
            }
            start = online != null ? online.getStartAt() : null;
            end = online != null ? online.getEndAt() : null;
            if (start == null && end == null) {
                return null;
            }
            date = start != null ? start.toLocalDate() : end.toLocalDate();
        }

        String phase = resolvePhase(status, start, end, now);
        int classroomCount = exam.getClassrooms() != null ? exam.getClassrooms().size() : 0;

        return TeacherDashboardCalendarEventResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .kind(kind)
                .phase(phase)
                .date(date)
                .startAt(toOffset(start))
                .endAt(toOffset(end))
                .subjectId(exam.getSubject() != null ? exam.getSubject().getId() : null)
                .subjectName(exam.getSubject() != null ? exam.getSubject().getSubjectName() : null)
                .classroomCount(classroomCount)
                .status(status)
                .build();
    }

    private static ExamStatus resolveOmrPhaseStatus(LocalDate examDate, LocalDate today) {
        if (examDate.isAfter(today)) {
            return ExamStatus.UPCOMING;
        }
        if (examDate.isEqual(today)) {
            return ExamStatus.ONGOING;
        }
        return ExamStatus.COMPLETED;
    }

    private static String resolvePhase(
            ExamStatus status, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (status == ExamStatus.CLOSED || status == ExamStatus.COMPLETED) {
            return "CLOSED";
        }
        if (status == ExamStatus.UPCOMING) {
            return "UPCOMING";
        }
        if (status == ExamStatus.ONGOING) {
            if (end != null && !end.isBefore(now)
                    && ChronoUnit.HOURS.between(now, end) <= ENDING_SOON_HOURS) {
                return "ENDING_SOON";
            }
            return "ONGOING";
        }
        if (start != null && now.isBefore(start)) {
            return "UPCOMING";
        }
        if (end != null && now.isAfter(end)) {
            return "CLOSED";
        }
        if (end != null && ChronoUnit.HOURS.between(now, end) <= ENDING_SOON_HOURS) {
            return "ENDING_SOON";
        }
        return "ONGOING";
    }

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.atZone(VN).toOffsetDateTime();
    }
}
