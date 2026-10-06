package com.examly.springapp.dto;

import java.time.LocalDateTime;

/** Request and response shapes of the instructor API. */
public final class InstructorDtos {
    private InstructorDtos() {
    }

    /**
     * Public instructor registration: the account fields plus the application
     * details.
     */
    public record InstructorRegisterRequest(String email, String password, String username, String mobileNumber,
            String qualification, Integer experienceYears, String expertise,
            String bio, String profileLink) {
    }

    /**
     * The editable application details (used to fix a pending or rejected
     * application).
     */
    public record InstructorDetails(String qualification, Integer experienceYears, String expertise,
            String bio, String profileLink) {
    }

    public record RejectRequest(String reason) {
    }

    /** What the instructor and the admin see. The password never appears here. */
    public record InstructorView(Long userId, String username, String email, String mobileNumber,
            String qualification, int experienceYears, String expertise, String bio,
            String profileLink, String status, String rejectionReason,
            LocalDateTime submittedAt, LocalDateTime reviewedAt, String reviewedBy) {
    }

    /**
     * Body of POST /api/instructor/status (public): the instructor's username or
     * e-mail.
     */
    public record ApplicationStatusRequest(String identifier) {
    }

    /**
     * What anyone may see about an application when they know the username or
     * e-mail: no phone number, no
     * bio, a masked e-mail.
     * mobileCheckOn says whether the server asks instructors to verify their
     * mobile; mobileVerified is
     * only meaningful then.
     * canLogin is true only for an APPROVED instructor whose mobile (when checked)
     * is verified.
     */
    public record ApplicationStatusView(String username, String maskedEmail, String status, LocalDateTime submittedAt,
            LocalDateTime reviewedAt, String rejectionReason, boolean mobileCheckOn,
            boolean mobileVerified, boolean canLogin) {
    }

    /**
     * Body of POST /api/instructor/resubmit (public): e-mail and password prove who
     * is fixing the
     * application.
     */
    public record ResubmitRequest(String email, String password, String qualification, Integer experienceYears,
            String expertise, String bio, String profileLink) {
    }
}
