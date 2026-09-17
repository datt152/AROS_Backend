package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.response.dashboard.StudentDashboardResponse;

import java.time.LocalDate;

public interface StudentDashboardService {

    /**
     * Dashboard SV: stats + actions + recentResults + calendar.
     * from/to chỉ lọc calendarEvents; null → đầu tháng trước → cuối tháng sau.
     */
    StudentDashboardResponse getDashboard(LocalDate from, LocalDate to);
}
