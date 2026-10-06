package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised at login when the password is right but the instructor application has
 * not been approved yet
 * (still PENDING) or
 * was REJECTED. Answered with 403. The message is fixed, safe to show, and
 * always starts with "Your
 * instructor application"
 * so the login page can recognise it and point the person to the status check
 * on the instructor
 * registration page.
 */
public class InstructorNotApprovedException extends AppException {
    public static final String PENDING_MESSAGE = "Your instructor application is still under review. You can log in as soon as an administrator approves it.";
    public static final String REJECTED_MESSAGE = "Your instructor application was not approved. Open the instructor registration page, check the reason and send it again.";

    public InstructorNotApprovedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}