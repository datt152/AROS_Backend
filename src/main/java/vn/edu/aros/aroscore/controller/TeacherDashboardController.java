package vn.edu.aros.aroscore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.response.dashboard.TeacherDashboardResponse;
import vn.edu.aros.aroscore.service.TeacherDashboardService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;

    /**
     * Dashboard GV — 1 round-trip: stats + todos + calendar.
     * from/to (YYYY-MM-DD) tuỳ chọn; mặc định đầu tháng trước → cuối tháng sau.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(teacherDashboardService.getDashboard(from, to));
    }
}
