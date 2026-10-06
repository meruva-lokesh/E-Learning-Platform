package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when an AI feature cannot work right now: no Gemini key is configured, or the AI answered
 * with something we cannot use (answered with 503). The message is safe to show to the customer.
 */
public class AiUnavailableException extends AppException {
    public AiUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
