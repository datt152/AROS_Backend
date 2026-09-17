package vn.edu.aros.aroscore.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardActionResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardCalendarEventResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardRecentResultResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardResponse;
import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardStatsResponse;
import vn.edu.aros.aroscore.entity.Classroom;
import vn.edu.aros.aroscore.entity.Exam;
import vn.edu.aros.aroscore.entity.Submission;
import vn.edu.aros.aroscore.entity.User;
import vn.edu.aros.aroscore.entity.enums.ExamPurpose;
import vn.edu.aros.aroscore.entity.enums.ExamStatus;
import vn.edu.aros.aroscore.entity.enums.GradingStatus;
import vn.edu.aros.aroscore.repository.ClassroomRepository;
import vn.edu.aros.aroscore.repository.ExamRepository;
import vn.edu.aros.aroscore.repository.SubmissionRepository;
import vn.edu.aros.aroscore.repository.UserRepository;
import vn.edu.aros.aroscore.service.StudentDashboardService;
import vn.edu.aros.aroscore.service.exam.ExamSettingsSupport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int ENDING_SOON_HOURS = 24;
    private static final int MAX_ACTIONS = 10;
    private static final int MAX_RECENT_RESULTS = 5;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ClassroomRepository classroomRepository;
    private final SubmissionRepository submissionRepository;

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardResponse getDashboard(LocalDate from, LocalDate to) {
        User student = userRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin học sinh!"));

        LocalDate today = LocalDate.now(VN);
        LocalDate rangeFrom = from != null ? from : today.minusMonths(1).withDayOfMonth(1);
        LocalDate rangeTo = to != null
                ? to
                : today.plusMonths(1).withDayOfMonth(today.plusMonths(1).lengthOfMonth());
        if (rangeTo.isBefore(rangeFrom)) {
            throw new IllegalArgumentException("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu!");
        }

        Set<Long> myClassroomIds = new HashSet<>(
                classroomRepository.findAllActiveByStudentId(student.getId(), PageRequest.of(0, 500))
                        .map(Classroom::getId)
                        .getContent());

        List<Exam> exams = examRepository.findOnlineAvailableForStudentDashboard(
                student.getId(), ExamStatus.DRAFT);

        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<String, List<Submission>> submissionsByExamClass = new HashMap<>();
        Map<Long, List<Submission>> submissionsByExam = new HashMap<>();
        if (!examIds.isEmpty()) {
            for (Submission s : submissionRepository.findAllByStudentIdAndExamIdIn(student.getId(), examIds)) {
                submissionsByExam.computeIfAbsent(s.getExam().getId(), k -> new ArrayList<>()).add(s);
                if (s.getClassroom() != null) {
                    String key = s.getExam().getId() + ":" + s.getClassroom().getId();
                    submissionsByExamClass.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now(VN);
        List<ExamClassroomContext> contexts = new ArrayList<>();
        for (Exam exam : exams) {
            if (exam.getClassrooms() == null) {
                continue;
            }
            for (Classroom classroom : exam.getClassrooms()) {
                if (classroom.getId() == null || !myClassroomIds.contains(classroom.getId())) {
                    continue;
                }
                if (Boolean.FALSE.equals(classroom.getIsActive())) {
                    continue;
                }
                String key = exam.getId() + ":" + classroom.getId();
                List<Submission> subs = submissionsByExamClass.getOrDefault(
                        key, submissionsByExam.getOrDefault(exam.getId(), List.of()).stream()
                                .filter(s -> s.getClassroom() != null
                                        && classroom.getId().equals(s.getClassroom().getId()))
                                .toList());
                contexts.add(buildContext(exam, classroom, subs, now));
            }
        }

        List<StudentDashboardActionResponse> actions = buildActions(contexts, now);
        List<StudentDashboardRecentResultResponse> recentResults = buildRecentResults(student.getId());
        List<StudentDashboardCalendarEventResponse> calendarEvents =
                buildCalendarEvents(contexts, rangeFrom, rangeTo, now);
        StudentDashboardStatsResponse stats = buildStats(contexts, student.getId(), now);

        return StudentDashboardResponse.builder()
                .generatedAt(Instant.now())
                .stats(stats)
                .actions(actions)
                .recentResults(recentResults)
                .calendarEvents(calendarEvents)
                .build();
    }

    private ExamClassroomContext buildContext(
            Exam exam, Classroom classroom, List<Submission> submissions, LocalDateTime now) {
        Submission preferred = null;
        for (Submission s : submissions) {
            if (preferred == null || preferSubmission(s, preferred)) {
                preferred = s;
            }
        }

        boolean timeLimited = ExamSettingsSupport.isTimeLimitEnabled(exam);
        GradingStatus myStatus = GradingStatus.NOT_STARTED;
        if (preferred != null) {
            if (preferred.getSubmitTime() != null) {
                myStatus = GradingStatus.SUBMITTED;
            } else if (timeLimited && isPastExamDuration(preferred.getStartTime(), exam.getDuration(), now)) {
                myStatus = GradingStatus.EXPIRED;
            } else {
                myStatus = GradingStatus.IN_PROGRESS;
            }
        }

        long attemptsUsed = countAttemptsUsed(submissions, exam, timeLimited, now);
        Integer maxAttempts = ExamSettingsSupport.effectiveMaxAttempts(exam);
        boolean examOpen = isExamScheduleOpen(exam, now);
        boolean canTake = false;
        if (examOpen) {
            if (preferred != null && preferred.getSubmitTime() == null) {
                if (myStatus == GradingStatus.IN_PROGRESS) {
                    canTake = true;
                } else if (myStatus == GradingStatus.EXPIRED) {
                    canTake = maxAttempts == null || attemptsUsed < maxAttempts;
                }
            } else {
                canTake = maxAttempts == null || attemptsUsed < maxAttempts;
            }
        }

        ExamStatus examStatus = ExamSettingsSupport.onlineStatus(exam);
        LocalDateTime startAt = ExamSettingsSupport.startAt(exam);
        LocalDateTime endAt = ExamSettingsSupport.endAt(exam);
        String phase = resolvePhase(examStatus, startAt, endAt, now);

        return new ExamClassroomContext(
                exam,
                classroom.getId(),
                classroom.getClassName(),
                myStatus,
                canTake,
                preferred,
                (int) attemptsUsed,
                maxAttempts,
                examStatus,
                startAt,
                endAt,
                phase,
                examOpen);
    }

    private StudentDashboardStatsResponse buildStats(
            List<ExamClassroomContext> contexts, Long studentId, LocalDateTime now) {
        long inProgress = contexts.stream()
                .filter(c -> c.myStatus == GradingStatus.IN_PROGRESS)
                .count();

        long upcomingExam = contexts.stream()
                .filter(c -> c.exam.getPurpose() == ExamPurpose.EXAM)
                .filter(c -> c.examStatus == ExamStatus.UPCOMING || c.examStatus == ExamStatus.ONGOING)
                .filter(c -> c.myStatus != GradingStatus.SUBMITTED)
                .filter(c -> c.canTake || c.myStatus == GradingStatus.NOT_STARTED
                        || c.myStatus == GradingStatus.IN_PROGRESS
                        || c.examStatus == ExamStatus.UPCOMING)
                .count();

        long openPractice = contexts.stream()
                .filter(c -> c.exam.getPurpose() == ExamPurpose.PRACTICE)
                .filter(c -> c.canTake)
                .count();

        long recentSubmitted = submissionRepository.countSubmittedByStudentSince(
                studentId, now.minusDays(7));

        return StudentDashboardStatsResponse.builder()
                .inProgressCount(inProgress)
                .upcomingExamCount(upcomingExam)
                .openPracticeCount(openPractice)
                .recentSubmittedCount(recentSubmitted)
                .build();
    }

    private List<StudentDashboardActionResponse> buildActions(
            List<ExamClassroomContext> contexts, LocalDateTime now) {
        List<StudentDashboardActionResponse> actions = new ArrayList<>();

        for (ExamClassroomContext ctx : contexts) {
            Exam exam = ctx.exam;
            boolean practice = exam.getPurpose() == ExamPurpose.PRACTICE;

            if (ctx.myStatus == GradingStatus.IN_PROGRESS) {
                actions.add(StudentDashboardActionResponse.builder()
                        .id("continue-" + exam.getId() + "-" + ctx.classroomId)
                        .type(practice ? "CONTINUE_PRACTICE" : "CONTINUE_EXAM")
                        .priority("HIGH")
                        .title(exam.getTitle())
                        .detail(buildContinueDetail(ctx, now))
                        .entityId(exam.getId())
                        .classroomId(ctx.classroomId)
                        .actionPath("/student/exams/" + exam.getId() + "/take?classroomId=" + ctx.classroomId)
                        .actionLabel("Tiếp tục làm bài")
                        .build());
                continue;
            }

            if (!practice
                    && (ctx.examStatus == ExamStatus.UPCOMING || ctx.examStatus == ExamStatus.ONGOING)
                    && ctx.myStatus != GradingStatus.SUBMITTED
                    && ctx.myStatus != GradingStatus.EXPIRED) {
                actions.add(StudentDashboardActionResponse.builder()
                        .id("upcoming-" + exam.getId() + "-" + ctx.classroomId)
                        .type("UPCOMING_EXAM")
                        .priority(ctx.phase.equals("ENDING_SOON") ? "HIGH" : "MEDIUM")
                        .title(exam.getTitle())
                        .detail(buildUpcomingDetail(ctx, now))
                        .entityId(exam.getId())
                        .classroomId(ctx.classroomId)
                        .actionPath("/student/exams/" + exam.getId() + "?classroomId=" + ctx.classroomId)
                        .actionLabel(ctx.canTake ? "Vào thi" : "Xem lịch")
                        .build());
            }

            if (practice && ctx.canTake) {
                actions.add(StudentDashboardActionResponse.builder()
                        .id("practice-" + exam.getId() + "-" + ctx.classroomId)
                        .type("OPEN_PRACTICE")
                        .priority(ctx.phase.equals("ENDING_SOON") ? "MEDIUM" : "LOW")
                        .title(exam.getTitle())
                        .detail(buildPracticeDetail(ctx, now))
                        .entityId(exam.getId())
                        .classroomId(ctx.classroomId)
                        .actionPath("/student/exams/" + exam.getId() + "/take?classroomId=" + ctx.classroomId)
                        .actionLabel("Làm bài luyện")
                        .build());
            }
        }

        actions.sort(Comparator
                .comparingInt((StudentDashboardActionResponse a) -> actionTypeRank(a.getType()))
                .thenComparing(StudentDashboardActionResponse::getId));

        if (actions.size() > MAX_ACTIONS) {
            return new ArrayList<>(actions.subList(0, MAX_ACTIONS));
        }
        return actions;
    }

    private List<StudentDashboardRecentResultResponse> buildRecentResults(Long studentId) {
        List<Submission> recent = submissionRepository.findSubmittedByStudentSince(
                studentId, LocalDateTime.now(VN).minusDays(30));
        List<StudentDashboardRecentResultResponse> results = new ArrayList<>();
        for (Submission s : recent) {
            if (results.size() >= MAX_RECENT_RESULTS) {
                break;
            }
            Exam exam = s.getExam();
            if (exam == null || exam.getExamMode() == vn.edu.aros.aroscore.entity.enums.ExamMode.OMR_PAPER) {
                continue;
            }
            boolean showScore = ExamSettingsSupport.showScoreToStudent(exam);
            String scorePart = showScore && s.getScore() != null
                    ? String.format(Locale.US, " · %.1f/%.0f", s.getScore(),
                    exam.getMaxScore() != null ? exam.getMaxScore() : 10.0)
                    : "";
            results.add(StudentDashboardRecentResultResponse.builder()
                    .id("sub-" + s.getId())
                    .occurredAt(s.getSubmitTime().atZone(VN).toInstant())
                    .type(showScore && s.getScore() != null ? "GRADED" : "SUBMITTED")
                    .message("Nộp «" + exam.getTitle() + "»" + scorePart)
                    .entityType("SUBMISSION")
                    .entityId(s.getId())
                    .build());
        }
        return results;
    }

    private List<StudentDashboardCalendarEventResponse> buildCalendarEvents(
            List<ExamClassroomContext> contexts,
            LocalDate from,
            LocalDate to,
            LocalDateTime now) {
        // Một event / exam / ngày — chọn classroom đầu tiên theo id
        Map<Long, ExamClassroomContext> byExam = new HashMap<>();
        for (ExamClassroomContext ctx : contexts) {
            byExam.merge(ctx.exam.getId(), ctx, (a, b) ->
                    a.classroomId <= b.classroomId ? a : b);
        }

        List<StudentDashboardCalendarEventResponse> events = new ArrayList<>();
        for (ExamClassroomContext ctx : byExam.values()) {
            LocalDateTime start = ctx.startAt;
            LocalDateTime end = ctx.endAt;
            if (start == null && end == null) {
                continue;
            }
            LocalDate date = start != null ? start.toLocalDate() : end.toLocalDate();
            if (date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            String kind = ctx.exam.getPurpose() == ExamPurpose.PRACTICE ? "PRACTICE" : "ONLINE";
            events.add(StudentDashboardCalendarEventResponse.builder()
                    .id(ctx.exam.getId())
                    .title(ctx.exam.getTitle())
                    .kind(kind)
                    .phase(ctx.phase)
                    .date(date)
                    .startAt(toOffset(start))
                    .endAt(toOffset(end))
                    .subjectId(ctx.exam.getSubject() != null ? ctx.exam.getSubject().getId() : null)
                    .subjectName(ctx.exam.getSubject() != null ? ctx.exam.getSubject().getSubjectName() : null)
                    .classroomId(ctx.classroomId)
                    .classroomCount(1)
                    .status(ctx.examStatus)
                    .myStatus(ctx.myStatus)
                    .build());
        }

        events.sort(Comparator
                .comparing(StudentDashboardCalendarEventResponse::getDate)
                .thenComparing(e -> e.getStartAt() != null ? e.getStartAt().toLocalDateTime() : LocalDateTime.MIN));
        return events;
    }

    private String buildContinueDetail(ExamClassroomContext ctx, LocalDateTime now) {
        StringBuilder sb = new StringBuilder("Đang làm dở");
        if (ctx.classroomName != null) {
            sb.append(" · Lớp ").append(ctx.classroomName);
        }
        if (ctx.endAt != null) {
            sb.append(" · hết hạn ").append(formatDeadline(ctx.endAt, now));
        } else if (ctx.preferred != null && ctx.preferred.getStartTime() != null
                && ExamSettingsSupport.isTimeLimitEnabled(ctx.exam)
                && ctx.exam.getDuration() != null) {
            LocalDateTime due = ctx.preferred.getStartTime().plusMinutes(ctx.exam.getDuration());
            sb.append(" · còn đến ").append(due.format(TIME_FMT));
        }
        if ("ENDING_SOON".equals(ctx.phase)) {
            sb.append(" · sắp hết hạn");
        }
        return sb.toString();
    }

    private String buildUpcomingDetail(ExamClassroomContext ctx, LocalDateTime now) {
        StringBuilder sb = new StringBuilder();
        if (ctx.classroomName != null) {
            sb.append("Lớp ").append(ctx.classroomName);
        }
        if (ctx.examStatus == ExamStatus.UPCOMING && ctx.startAt != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("mở ").append(formatDeadline(ctx.startAt, now));
        } else if (ctx.endAt != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("hết hạn ").append(formatDeadline(ctx.endAt, now));
        }
        if ("ENDING_SOON".equals(ctx.phase)) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("sắp kết thúc");
        }
        return sb.isEmpty() ? "Kỳ thi sắp tới / đang mở" : sb.toString();
    }

    private String buildPracticeDetail(ExamClassroomContext ctx, LocalDateTime now) {
        StringBuilder sb = new StringBuilder("Luyện tập đang mở");
        if (ctx.classroomName != null) {
            sb.append(" · Lớp ").append(ctx.classroomName);
        }
        Integer max = ctx.maxAttempts;
        if (max != null) {
            sb.append(" · còn ").append(Math.max(0, max - ctx.attemptsUsed)).append("/").append(max).append(" lượt");
        }
        if (ctx.endAt != null && "ENDING_SOON".equals(ctx.phase)) {
            sb.append(" · sắp hết hạn ").append(formatDeadline(ctx.endAt, now));
        }
        return sb.toString();
    }

    private static String formatDeadline(LocalDateTime at, LocalDateTime now) {
        if (at.toLocalDate().equals(now.toLocalDate())) {
            return "hôm nay " + at.format(TIME_FMT);
        }
        return at.format(DAY_FMT) + " " + at.format(TIME_FMT);
    }

    private static int actionTypeRank(String type) {
        return switch (type) {
            case "CONTINUE_EXAM", "CONTINUE_PRACTICE" -> 0;
            case "UPCOMING_EXAM" -> 1;
            case "OPEN_PRACTICE" -> 2;
            default -> 9;
        };
    }

    private static boolean isExamScheduleOpen(Exam exam, LocalDateTime now) {
        ExamStatus status = ExamSettingsSupport.onlineStatus(exam);
        if (status == ExamStatus.CLOSED || status == ExamStatus.COMPLETED || status == ExamStatus.DRAFT) {
            return false;
        }
        LocalDateTime startAt = ExamSettingsSupport.startAt(exam);
        LocalDateTime endAt = ExamSettingsSupport.endAt(exam);
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return false;
        }
        // UPCOMING nhưng chưa tới startAt đã chặn; ONGOING/UPCOMING trong cửa sổ OK
        return status == ExamStatus.ONGOING || status == ExamStatus.UPCOMING;
    }

    private static String resolvePhase(
            ExamStatus status, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (status == ExamStatus.CLOSED || status == ExamStatus.COMPLETED) {
            return "CLOSED";
        }
        if (status == ExamStatus.UPCOMING || (start != null && now.isBefore(start))) {
            return "UPCOMING";
        }
        if (end != null && now.isAfter(end)) {
            return "CLOSED";
        }
        if (status == ExamStatus.ONGOING || (start == null || !now.isBefore(start))) {
            if (end != null && !end.isBefore(now)
                    && ChronoUnit.HOURS.between(now, end) <= ENDING_SOON_HOURS) {
                return "ENDING_SOON";
            }
            return "ONGOING";
        }
        return "ONGOING";
    }

    private static boolean preferSubmission(Submission candidate, Submission current) {
        boolean candidateSubmitted = candidate.getSubmitTime() != null;
        boolean currentSubmitted = current.getSubmitTime() != null;
        if (candidateSubmitted != currentSubmitted) {
            return candidateSubmitted;
        }
        int candidateAttempt = candidate.getAttemptNo() != null ? candidate.getAttemptNo() : 1;
        int currentAttempt = current.getAttemptNo() != null ? current.getAttemptNo() : 1;
        return candidateAttempt >= currentAttempt;
    }

    private static boolean isPastExamDuration(
            LocalDateTime startTime, Integer durationMinutes, LocalDateTime now) {
        if (startTime == null || durationMinutes == null) {
            return false;
        }
        return now.isAfter(startTime.plusMinutes(durationMinutes));
    }

    /** Đã nộp + draft hết giờ (chưa nộp) đều tính là đã dùng lượt. */
    private static long countAttemptsUsed(
            List<Submission> submissions, Exam exam, boolean timeLimited, LocalDateTime now) {
        return submissions.stream().filter(s -> {
            if (s.getSubmitTime() != null) {
                return true;
            }
            return timeLimited && isPastExamDuration(s.getStartTime(), exam.getDuration(), now);
        }).count();
    }

    private static OffsetDateTime toOffset(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.atZone(VN).toOffsetDateTime();
    }

    private record ExamClassroomContext(
            Exam exam,
            Long classroomId,
            String classroomName,
            GradingStatus myStatus,
            boolean canTake,
            Submission preferred,
            int attemptsUsed,
            Integer maxAttempts,
            ExamStatus examStatus,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String phase,
            boolean examOpen) {
    }
}
