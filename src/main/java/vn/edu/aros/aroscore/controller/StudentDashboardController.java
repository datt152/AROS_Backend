package vn.edu.aros.aroscore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardResponse;
import vn.edu.aros.aroscore.service.StudentDashboardService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final StudentDashboardService studentDashboardService;

    /**
     * Dashboard SV — Online + Practice only (không OMR).
     * from/to lọc calendar; stats/actions/recentResults theo thời điểm hiện tại.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(studentDashboardService.getDashboard(from, to));
    }
}
