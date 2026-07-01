package vn.edu.aros.aroscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.aros.aroscore.entity.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
}