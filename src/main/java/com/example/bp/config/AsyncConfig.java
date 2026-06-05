package com.example.bp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} (PRD §10). Mail sending is currently synchronous so the
 * contact/OTP flows can surface success/failure inline; heavy or retry-prone work
 * can be moved to {@code @Async} methods or a job table later.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
