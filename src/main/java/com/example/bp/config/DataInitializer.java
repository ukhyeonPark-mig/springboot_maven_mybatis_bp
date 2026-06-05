package com.example.bp.config;

import java.time.LocalDateTime;
import java.time.Year;

import com.example.bp.domain.Setting;
import com.example.bp.domain.User;
import com.example.bp.mapper.SettingMapper;
import com.example.bp.mapper.UserMapper;
import com.example.bp.support.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Idempotent seed (PRD §5.4): default admin + settings singleton.
 * Runs every startup but only inserts what is missing, so it is safe to keep on.
 *
 * <p><b>⚠ Change the seeded admin password before any production deployment.</b>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "password1!"; // ⚠ seed only — change in prod

    // BCrypt cost 12 (PRD §6.1). Reused standalone here; SecurityConfig exposes the bean in PR2.
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private final UserMapper userMapper;
    private final SettingMapper settingMapper;
    private final AppProperties appProperties;

    public DataInitializer(UserMapper userMapper, SettingMapper settingMapper, AppProperties appProperties) {
        this.userMapper = userMapper;
        this.settingMapper = settingMapper;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedSettings();
    }

    private void seedAdmin() {
        if (userMapper.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        User admin = new User();
        admin.setRole("admin");
        admin.setEmail(ADMIN_EMAIL);
        admin.setName("관리자");
        admin.setPassword(ENCODER.encode(ADMIN_PASSWORD));
        admin.setEmailVerifiedAt(LocalDateTime.now());
        admin.setOtpAttempts(0);
        userMapper.insert(admin);
        log.warn("Seeded default admin {} — change this password before production.", ADMIN_EMAIL);
    }

    private void seedSettings() {
        if (settingMapper.findFirst() != null) {
            return;
        }
        Setting setting = new Setting();
        setting.setFooter("© " + Year.now() + " " + appProperties.name());
        setting.setVersion("1.0.0");
        settingMapper.insert(setting);
        log.info("Seeded default settings row.");
    }
}
