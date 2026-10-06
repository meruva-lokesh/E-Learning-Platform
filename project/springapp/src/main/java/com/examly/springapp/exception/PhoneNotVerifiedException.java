package com.examly.springapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised at login when the password is right but the mobile number has not been verified with an OTP yet
 * (answered with 403). The message is fixed and safe to show; the frontend looks for it to open the
 * "Verify your number" page.
 */
public class PhoneNotVerifiedException extends AppException {
    public static final String MESSAGE = "Please verify your mobile number to continue";

    public PhoneNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, MESSAGE);
    }
}
