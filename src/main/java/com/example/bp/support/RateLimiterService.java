package com.example.bp.support;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

/**
 * Per (action + IP) rate limiting (PRD §8.3), replacing Laravel's
 * {@code RateLimiter}. Each key gets a Bucket4j fixed-window bucket
 * (capacity = maxAttempts, refilled wholesale every window). Single-node /
 * in-memory — fine for the boilerplate; swap for a distributed store if scaled.
 */
@Service
public class RateLimiterService {

    /** Outcome of an attempt: allowed, or blocked with seconds until retry. */
    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public String key(String action, String ip) {
        return "signin-component:" + action + ":" + ip;
    }

    /**
     * Mirrors the reference flow "tooManyAttempts → else hit": consumes one token
     * if available; otherwise reports the wait time without consuming.
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

    /** Clear a key after a successful action (e.g. successful login). */
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
