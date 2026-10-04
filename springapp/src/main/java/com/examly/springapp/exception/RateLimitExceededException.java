package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a client sends too many requests (answered with 429 and a Retry-After header).
 *
 * @author Team Lead
 */
public class RateLimitExceededException extends AppException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
