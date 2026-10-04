package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a call to an external API (e.g. Gemini) fails (answered with 502).
 * The technical message stays in the logs; clients only see a generic text.
 *
 * @author Team Lead
 */
public class ApiCommunicationException extends AppException {
    public ApiCommunicationException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }

    @Override
    public String getClientMessage() {
        return "An upstream service is unavailable";
    }
}
