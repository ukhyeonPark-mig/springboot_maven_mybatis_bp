package com.example.bp.config;

import com.example.bp.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled operations (PRD §10). Backups run only in prod so local dev doesn't
 * spawn mysqldump; retention keeps 14 days.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Profile("prod")
    static class BackupTasks {
        private static final Logger log = LoggerFactory.getLogger(BackupTasks.class);
        private static final int RETENTION_DAYS = 14;

        private final BackupService backupService;

        BackupTasks(BackupService backupService) {
            this.backupService = backupService;
        }

        @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
        void dailyBackup() {
            try {
                log.info("Scheduled backup created: {}", backupService.create());
            } catch (Exception e) {
                log.error("Scheduled backup failed", e);
            }
        }

        @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Seoul")
        void cleanup() {
            int removed = backupService.cleanup(RETENTION_DAYS);
            if (removed > 0) {
                log.info("Removed {} backups older than {} days", removed, RETENTION_DAYS);
            }
        }
    }
}
