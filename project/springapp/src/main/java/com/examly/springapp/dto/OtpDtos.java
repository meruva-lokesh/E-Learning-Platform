package com.examly.springapp.dto;

/** Request and response shapes of the OTP endpoints. */
public final class OtpDtos {
    private OtpDtos() {}

    public record OtpSendRequest(String email) {}

    public record OtpVerifyRequest(String email, String code) {}

    /**
     * required = false means the platform does not ask for OTP (nothing was sent).
     * devCode is filled only in development mode with otp.dev-expose-code=true.
     */
    public record OtpSendResponse(boolean required, String message, int resendInSeconds, String devCode) {}

    public record OtpVerifyResponse(boolean verified, String message) {}
}
