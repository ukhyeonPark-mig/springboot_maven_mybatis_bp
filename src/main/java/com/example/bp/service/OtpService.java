package com.example.bp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

/**
 * OTP primitives for password reset (PRD §FR-2.3): 6-digit code, 10-minute
 * expiry, max 5 attempts, constant-time comparison. Orchestration (rate limit,
 * user-row updates, flash messages) lives in the signin controller.
 */
@Service
public class OtpService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration TTL = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();

    /** 6-digit code in [100000, 999999] (matches reference {@code rand(100000, 999999)}). */
    public String generate() {
        return String.valueOf(100_000 + random.nextInt(900_000));
    }

    public LocalDateTime expiryFromNow() {
        return LocalDateTime.now().plus(TTL);
    }

    public boolean isExpired(LocalDateTime expiresAt) {
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean attemptsExceeded(int attempts) {
        return attempts >= MAX_ATTEMPTS;
    }

    public int remainingAttempts(int attempts) {
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    /** Constant-time comparison to avoid timing attacks (PRD §FR-2.3). */
    public boolean matches(String stored, String input) {
        if (stored == null || input == null) {
            return false;
        }
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                input.getBytes(StandardCharsets.UTF_8));
    }
}
