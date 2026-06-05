package com.example.bp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/** OTP rules per PRD §FR-2.3 / acceptance criteria (6 digits, 10-min, 5 attempts, constant-time). */
class OtpServiceTest {

    private final OtpService otp = new OtpService();

    @RepeatedTest(200)
    void generatesSixDigitCode() {
        String code = otp.generate();
        assertThat(code).hasSize(6).containsPattern("^[0-9]{6}$");
        int value = Integer.parseInt(code);
        assertThat(value).isBetween(100_000, 999_999);
    }

    @Test
    void expiryIsAboutTenMinutesAhead() {
        LocalDateTime expiry = otp.expiryFromNow();
        assertThat(expiry).isAfter(LocalDateTime.now().plusMinutes(9))
                .isBefore(LocalDateTime.now().plusMinutes(11));
    }

    @Test
    void isExpiredHandlesPastFutureAndNull() {
        assertThat(otp.isExpired(LocalDateTime.now().minusMinutes(1))).isTrue();
        assertThat(otp.isExpired(LocalDateTime.now().plusMinutes(1))).isFalse();
        assertThat(otp.isExpired(null)).isTrue();
    }

    @Test
    void attemptLimitAtFive() {
        assertThat(otp.attemptsExceeded(4)).isFalse();
        assertThat(otp.attemptsExceeded(5)).isTrue();
        assertThat(otp.attemptsExceeded(6)).isTrue();

        assertThat(otp.remainingAttempts(0)).isEqualTo(5);
        assertThat(otp.remainingAttempts(3)).isEqualTo(2);
        assertThat(otp.remainingAttempts(5)).isZero();
        assertThat(otp.remainingAttempts(6)).isZero();
    }

    @Test
    void constantTimeMatch() {
        assertThat(otp.matches("123456", "123456")).isTrue();
        assertThat(otp.matches("123456", "654321")).isFalse();
        assertThat(otp.matches("123456", "12345")).isFalse();
        assertThat(otp.matches(null, "123456")).isFalse();
        assertThat(otp.matches("123456", null)).isFalse();
    }
}
