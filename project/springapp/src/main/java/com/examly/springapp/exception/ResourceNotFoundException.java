package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a requested record does not exist (answered with 404).
 *
 * @author Team Lead
 */
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
