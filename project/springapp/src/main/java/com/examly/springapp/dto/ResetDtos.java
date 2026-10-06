package com.examly.springapp.dto;

/** Request and response bodies of the forgot-password endpoints (direct reset: no e-mail is sent). */
public final class ResetDtos {
    private ResetDtos() {}

    /** Body of POST /api/forgot-password (step 1 of the page). */
    public record ForgotRequest(String email) {}

    /**
     * Answer of POST /api/forgot-password. It never says whether the e-mail has an account. requireMobile tells the page
     * whether step 2 must also ask for the registered mobile number (reset.require-mobile=true).
     */
    public record ForgotResponse(boolean requireMobile) {}

    /** Body of POST /api/reset-password (step 2). mobile is only needed when reset.require-mobile=true. */
    public record ResetRequest(String email, String newPassword, String mobile) {}

    /** Answer of POST /api/reset-password. */
    public record ResetResponse(String message) {}
}
