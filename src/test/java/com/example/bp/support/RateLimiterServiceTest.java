package com.example.bp.support;

import java.time.Duration;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * (action + IP)별 고정 윈도우 limiter를 PRD §8.3 임계값에 대해 검증한다.
 * Bucket4j는 인메모리이므로 각 테스트는 가득 찬 버킷에서 시작한다.
 */
class RateLimiterServiceTest {

    private final RateLimiterService limiter = new RateLimiterService();

    @Test
    void key_isNamespacedByActionAndIp() {
        assertThat(limiter.key("login", "1.2.3.4"))
                .isEqualTo("signin-component:login:1.2.3.4")
                .isNotEqualTo(limiter.key("signup", "1.2.3.4"));
    }

    /** PRD §8.3 — login 5/60초 · signup 3/300초 · reset-send 3/300초 · reset-verify 10/600초 · contact 3/600초. */
    @ParameterizedTest(name = "{0}: {1} attempts per {2}s")
    @CsvSource({
            "login,        5, 60",
            "signup,       3, 300",
            "reset-send,   3, 300",
            "reset-verify, 10, 600",
            "contact,      3, 600",
    })
    void allowsExactlyMaxAttemptsThenBlocks(String action, int max, long windowSeconds) {
        String key = limiter.key(action, "203.0.113.7");
        Duration window = Duration.ofSeconds(windowSeconds);

        // 처음 `max`회 시도는 허용된다.
        IntStream.range(0, max).forEach(i ->
                assertThat(limiter.attempt(key, max, window).allowed())
                        .as("attempt %d of %d for %s", i + 1, max, action)
                        .isTrue());

        // 다음 시도는 합리적인 retry-after(1..window)와 함께 차단된다.
        RateLimiterService.Result blocked = limiter.attempt(key, max, window);
        assertThat(blocked.allowed()).as("attempt %d for %s blocked", max + 1, action).isFalse();
        assertThat(blocked.retryAfterSeconds()).isBetween(1L, windowSeconds);
    }

    @Test
    void distinctIpsHaveIndependentBuckets() {
        Duration window = Duration.ofSeconds(60);
        String a = limiter.key("login", "10.0.0.1");
        String b = limiter.key("login", "10.0.0.2");

        IntStream.range(0, 5).forEach(i -> limiter.attempt(a, 5, window));
        assertThat(limiter.attempt(a, 5, window).allowed()).isFalse(); // a 소진됨
        assertThat(limiter.attempt(b, 5, window).allowed()).isTrue();  // b 영향 없음
    }

    @Test
    void reset_restoresFullAllowance() {
        Duration window = Duration.ofSeconds(60);
        String key = limiter.key("login", "198.51.100.9");

        IntStream.range(0, 5).forEach(i -> limiter.attempt(key, 5, window));
        assertThat(limiter.attempt(key, 5, window).allowed()).isFalse();

        limiter.reset(key); // 예: 로그인 성공 후

        assertThat(limiter.attempt(key, 5, window).allowed()).isTrue();
    }
}
