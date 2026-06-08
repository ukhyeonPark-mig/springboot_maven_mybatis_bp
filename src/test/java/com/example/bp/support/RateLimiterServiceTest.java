package com.example.bp.support;

import java.time.Duration;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the per (action + IP) fixed-window limiter against the PRD §8.3
 * thresholds. Bucket4j is in-memory so each test starts from a full bucket.
 */
class RateLimiterServiceTest {

    private final RateLimiterService limiter = new RateLimiterService();

    @Test
    void key_isNamespacedByActionAndIp() {
        assertThat(limiter.key("login", "1.2.3.4"))
                .isEqualTo("signin-component:login:1.2.3.4")
                .isNotEqualTo(limiter.key("signup", "1.2.3.4"));
    }

    /** PRD §8.3 — login 5/60s · signup 3/300s · reset-send 3/300s · reset-verify 10/600s · contact 3/600s. */
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

        // The first `max` attempts are allowed.
        IntStream.range(0, max).forEach(i ->
                assertThat(limiter.attempt(key, max, window).allowed())
                        .as("attempt %d of %d for %s", i + 1, max, action)
                        .isTrue());

        // The next one is blocked with a sane retry-after (1..window).
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
        assertThat(limiter.attempt(a, 5, window).allowed()).isFalse(); // a exhausted
        assertThat(limiter.attempt(b, 5, window).allowed()).isTrue();  // b untouched
    }

    @Test
    void reset_restoresFullAllowance() {
        Duration window = Duration.ofSeconds(60);
        String key = limiter.key("login", "198.51.100.9");

        IntStream.range(0, 5).forEach(i -> limiter.attempt(key, 5, window));
        assertThat(limiter.attempt(key, 5, window).allowed()).isFalse();

        limiter.reset(key); // e.g. after a successful login

        assertThat(limiter.attempt(key, 5, window).allowed()).isTrue();
    }
}
