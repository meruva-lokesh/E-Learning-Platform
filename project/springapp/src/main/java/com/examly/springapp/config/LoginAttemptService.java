package com.examly.springapp.config;

import com.examly.springapp.exception.RateLimitExceededException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Locks a (email, IP) pair for a while after too many wrong passwords.
 *
 * @author Suriya
 */
@Component
public class LoginAttemptService {
    private record Attempt(int failures, long lockedUntil) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long lockMs;

    public LoginAttemptService(@Value("${ratelimit.login.max-failures:5}") int maxFailures,
                               @Value("${ratelimit.login.lock-minutes:15}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockMs = lockMinutes * 60_000L;
    }

    public void checkNotLocked(String key) {
        Attempt a = attempts.get(key);
        long now = System.currentTimeMillis();
        if (a != null && a.lockedUntil() > now) {
            throw new RateLimitExceededException("Too many failed login attempts. Please try again later.",
                    (a.lockedUntil() - now) / 1000 + 1);
        }
    }

    public void recordFailure(String key) {
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, old) -> {
            boolean fresh = old == null || (old.lockedUntil() != 0 && old.lockedUntil() <= now);
            int failures = fresh ? 1 : old.failures() + 1;
            return new Attempt(failures, failures >= maxFailures ? now + lockMs : 0);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    @Scheduled(fixedDelay = 3_600_000L)
    public void purge() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> e.getValue().lockedUntil() != 0 && e.getValue().lockedUntil() < now);
        if (attempts.size() > 100_000) {
            attempts.clear();
        }
    }
}
