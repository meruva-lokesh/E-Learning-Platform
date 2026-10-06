package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of the application's exception hierarchy. Every business or infrastructure failure we raise
 * ourselves extends this class and carries the HTTP status it should be answered with, so
 * {@link GlobalExceptionHandler} can funnel all of them through one code path.
 *
 * <pre>
 * RuntimeException
 *   +-- AppException (abstract, carries HttpStatus)
 *         +-- ResourceNotFoundException    404
 *         +-- DuplicateResourceException   409
 *         +-- InvalidRequestException      400
 *         +-- RateLimitExceededException   429
 *         +-- ApiCommunicationException    502
 *         +-- DatabaseOperationException   500 (wraps DataAccessException)
 * </pre>
 *
 * @author Team Lead
 */
public abstract class AppException extends RuntimeException {
    private final HttpStatus status;

    protected AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected AppException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** The text that is safe to show to API clients. Subclasses hide internal details by overriding it. */
    public String getClientMessage() {
        return getMessage();
    }
}
