package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a record that must be unique already exists, e.g. an email (answered with 409).
 *
 * @author Team Lead
 */
public class DuplicateResourceException extends AppException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
