package com.combostrap.analyics.capture;

import com.combostrap.analyics.config.AnalyticsConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key token-bucket rate limiter backed by Bucket4j.
 * <p>
 * Best practice for an ingestion endpoint that anyone on the internet can hit is
 * a token bucket keyed by the caller: it absorbs short bursts (up to the bucket
 * capacity) while bounding the sustained rate (the refill). We key by api_key
 * when present (a tenant/app), falling back to client IP, so one noisy app can't
 * starve the others.
 * <p>
 * This holds buckets in-memory, which is the right scope for a single instance.
 * Behind several instances you would back the same {@link Bucket} abstraction
 * with a distributed store (e.g. bucket4j-redis) so the limit is shared.
 */
public class RateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long refillPerMinute;

    public RateLimiter(AnalyticsConfig config) {
        this.capacity = config.rateLimitCapacity();
        this.refillPerMinute = config.rateLimitRefillPerMinute();
    }

    /**
     * @return {@code true} if a request for {@code key} is allowed (a token was
     * consumed), {@code false} if the caller is over its limit.
     */
    public boolean tryAcquire(String key) {
        return buckets.computeIfAbsent(key, k -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(refillPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
