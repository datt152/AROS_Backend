package vn.edu.aros.aroscore.service;

import vn.edu.aros.aroscore.dto.response.dashboard.TeacherDashboardResponse;

import java.time.LocalDate;

public interface TeacherDashboardService {

    /**
     * Dashboard GV: stats + todos + calendar trong khoảng [from, to].
     * from/to null → mặc định đầu tháng trước → cuối tháng sau.
     */
    TeacherDashboardResponse getDashboard(LocalDate from, LocalDate to);
}
