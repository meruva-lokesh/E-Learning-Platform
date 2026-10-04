package com.examly.springapp.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.examly.springapp.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the failed-login lockout.
 *
 * @author Suriya
 */
class LoginAttemptServiceTest {

    @Test
    void locksAfterTooManyFailures() {
        LoginAttemptService svc = new LoginAttemptService(3, 15);
        svc.recordFailure("u|ip");
        svc.recordFailure("u|ip");
        assertDoesNotThrow(() -> svc.checkNotLocked("u|ip"));
        svc.recordFailure("u|ip");
        assertThrows(RateLimitExceededException.class, () -> svc.checkNotLocked("u|ip"));
    }

    @Test
    void successClearsTheFailures() {
        LoginAttemptService svc = new LoginAttemptService(2, 15);
        svc.recordFailure("u|ip");
        svc.recordSuccess("u|ip");
        svc.recordFailure("u|ip");
        assertDoesNotThrow(() -> svc.checkNotLocked("u|ip"));
    }

    @Test
    void otherUsersAreNotAffected() {
        LoginAttemptService svc = new LoginAttemptService(1, 15);
        svc.recordFailure("a|ip");
        assertThrows(RateLimitExceededException.class, () -> svc.checkNotLocked("a|ip"));
        assertDoesNotThrow(() -> svc.checkNotLocked("b|ip"));
    }
}
