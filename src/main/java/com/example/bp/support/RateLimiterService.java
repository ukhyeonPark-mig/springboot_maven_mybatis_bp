package com.example.bp.support;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

/**
 * 같은 행동을 정해진 시간 안에 제한되게 허용하도록 하여 무차별 대입 공격을 차단한다.
 *      --->  (action + IP)별 rate limiting (PRD §8.3)으로, Laravel의 {@code RateLimiter}를 대체한다. 
 * 
 * - 각 키는 Bucket4j fixed-window 버킷을 가진다
 * - (capacity = maxAttempts, 매 window마다 통째로 리필됨). 
 */
@Service
public class RateLimiterService {

    /** 시도 결과: 허용, 또는 재시도까지 남은 초와 함께 차단됨. */
    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public String key(String action, String ip) {
        return "signin-component:" + action + ":" + ip;
    }

    /**
     * 레퍼런스 플로우 "tooManyAttempts → else hit"를 모방한다: 가능하면 토큰
     * 하나를 소비하고, 그렇지 않으면 소비 없이 대기 시간을 보고한다.
     */
    public Result attempt(String key, int maxAttempts, Duration window) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(maxAttempts, window));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new Result(true, 0);
        }
        long seconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
        return new Result(false, Math.max(seconds, 1));
    }

    /** 성공한 액션 후 키를 비운다 (예: 로그인 성공). */
    public void reset(String key) {
        buckets.remove(key);
    }

    private Bucket newBucket(int maxAttempts, Duration window) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(maxAttempts)
                .refillIntervally(maxAttempts, window)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
