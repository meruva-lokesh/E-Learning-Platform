package com.examly.springapp.config;

import com.examly.springapp.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-user limits for the AI search so one account cannot burn the Gemini quota.
 *
 * @author Sumit
 */
@Component
public class AiUsageLimiter {
    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final RateLimiter limiter;
    private final int perMinute;
    private final int perDay;

    public AiUsageLimiter(RateLimiter limiter,
                          @Value("${ai.limit.per-minute:6}") int perMinute,
                          @Value("${ai.limit.per-day:60}") int perDay) {
        this.limiter = limiter;
        this.perMinute = perMinute;
        this.perDay = perDay;
    }

    public void check(String userKey) {
        String minuteKey = "ai:min:" + userKey;
        String dayKey = "ai:day:" + userKey;
        if (!limiter.tryAcquire(minuteKey, perMinute, MINUTE_MS)) {
            throw new RateLimitExceededException(
                    "AI search limit reached: at most " + perMinute + " searches per minute.",
                    limiter.retryAfterSeconds(minuteKey, MINUTE_MS));
        }
        if (!limiter.tryAcquire(dayKey, perDay, DAY_MS)) {
            throw new RateLimitExceededException(
                    "Daily AI search limit reached (" + perDay + " per day). Try again tomorrow.",
                    limiter.retryAfterSeconds(dayKey, DAY_MS));
        }
    }
}
