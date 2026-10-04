package com.examly.springapp.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the sliding-window rate limiter.
 *
 * @author Suriya
 */
class RateLimiterTest {

    @Test
    void allowsUpToTheLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("k", 3, 60_000));
        assertTrue(limiter.tryAcquire("k", 3, 60_000));
        assertTrue(limiter.tryAcquire("k", 3, 60_000));
        assertFalse(limiter.tryAcquire("k", 3, 60_000));
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("a", 1, 60_000));
        assertFalse(limiter.tryAcquire("a", 1, 60_000));
        assertTrue(limiter.tryAcquire("b", 1, 60_000));
    }

    @Test
    void allowsAgainAfterTheWindowPasses() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();
        assertTrue(limiter.tryAcquire("k", 1, 50));
        assertFalse(limiter.tryAcquire("k", 1, 50));
        Thread.sleep(80);
        assertTrue(limiter.tryAcquire("k", 1, 50));
    }
}
