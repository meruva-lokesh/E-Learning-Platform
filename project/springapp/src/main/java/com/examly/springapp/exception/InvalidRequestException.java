package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when the request is well-formed JSON but breaks a business or identity rule,
 * e.g. a non-positive id or an order without courses (answered with 400).
 *
 * @author Team Lead
 */
public class InvalidRequestException extends AppException {
    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
