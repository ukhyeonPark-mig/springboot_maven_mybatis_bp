package com.example.bp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

/**
 * 비밀번호 재설정용 OTP 기본 기능 (PRD §FR-2.3): 6자리 코드, 10분 만료,
 * 최대 5회 시도, 상수 시간 비교. 오케스트레이션(rate limit, 사용자 행 갱신,
 * 플래시 메시지)은 signin 컨트롤러에 있다.
 */
@Service
public class OtpService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration TTL = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();

    /** [100000, 999999] 범위의 6자리 코드 (원본의 {@code rand(100000, 999999)}와 동일). */
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

    /** 타이밍 공격을 방지하기 위한 상수 시간 비교 (PRD §FR-2.3). */
    public boolean matches(String stored, String input) {
        if (stored == null || input == null) {
            return false;
        }
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                input.getBytes(StandardCharsets.UTF_8));
    }
}
