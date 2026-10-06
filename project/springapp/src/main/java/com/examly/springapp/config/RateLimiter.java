package com.examly.springapp.config;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sliding-window rate limiter kept in memory. Fine for a single instance;
 * use Redis (or an API gateway) when the backend runs on several instances.
 *
 * @author Suriya
 */
@Component
public class RateLimiter {
    private static final long MAX_IDLE_MS = 24L * 60 * 60 * 1000;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Returns true and records the hit when the key is still under its limit for the window. */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() >= windowMs) {
                q.pollFirst();
            }
            if (q.size() >= limit) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    /** Seconds until the oldest hit leaves the window (at least 1). */
    public long retryAfterSeconds(String key, long windowMs) {
        Deque<Long> q = hits.get(key);
        if (q == null) return 1;
        synchronized (q) {
            if (q.isEmpty()) return 1;
            long wait = windowMs - (System.currentTimeMillis() - q.peekFirst());
            return Math.max(1, wait / 1000 + 1);
        }
    }

    @Scheduled(fixedDelay = 3_600_000L)
    public void purgeIdle() {
        long now = System.currentTimeMillis();
        hits.entrySet().removeIf(e -> {
            Deque<Long> q = e.getValue();
            synchronized (q) {
                return q.isEmpty() || now - q.peekLast() > MAX_IDLE_MS;
            }
        });
    }
}
