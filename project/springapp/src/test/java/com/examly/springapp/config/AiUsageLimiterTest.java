package com.examly.springapp.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.examly.springapp.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the per-user AI search limits.
 *
 * @author Sumit
 */
class AiUsageLimiterTest {

    @Test
    void blocksAfterThePerMinuteLimit() {
        AiUsageLimiter limiter = new AiUsageLimiter(new RateLimiter(), 2, 100);
        assertDoesNotThrow(() -> limiter.check("u1"));
        assertDoesNotThrow(() -> limiter.check("u1"));
        assertThrows(RateLimitExceededException.class, () -> limiter.check("u1"));
    }

    @Test
    void blocksAfterThePerDayLimit() {
        AiUsageLimiter limiter = new AiUsageLimiter(new RateLimiter(), 100, 2);
        assertDoesNotThrow(() -> limiter.check("u1"));
        assertDoesNotThrow(() -> limiter.check("u1"));
        assertThrows(RateLimitExceededException.class, () -> limiter.check("u1"));
    }

    @Test
    void eachUserHasTheirOwnQuota() {
        AiUsageLimiter limiter = new AiUsageLimiter(new RateLimiter(), 1, 10);
        assertDoesNotThrow(() -> limiter.check("u1"));
        assertDoesNotThrow(() -> limiter.check("u2"));
    }
}
