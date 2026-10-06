package com.examly.springapp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.dto.InstructorDtos.*;
import com.examly.springapp.dto.InstructorDtos.ApplicationStatusView;
import com.examly.springapp.dto.InstructorDtos.InstructorDetails;
import com.examly.springapp.dto.InstructorDtos.InstructorRegisterRequest;
import com.examly.springapp.dto.InstructorDtos.InstructorView;
import com.examly.springapp.dto.InstructorDtos.ResubmitRequest;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InstructorNotApprovedException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.InstructorProfile;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.InstructorProfileRepo;
import com.examly.springapp.repository.UserRepo;

/**
 * Instructor registration, the admin's approve / reject decision (a rejection
 * needs a reason) and the
 * "is this instructor approved?" check used before an instructor may manage
 * courses or videos.
 */
@Service
public class InstructorService {
    private static final Logger log = LoggerFactory.getLogger(InstructorService.class);
    private static final String PASSWORD_POLICY = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    static final int MIN_REASON = 5, MAX_REASON = 500;
    private final InstructorProfileRepo profiles;
    private final UserRepo users;
    private final PasswordEncoder encoder;
    private final OtpService otp;

    @Autowired
    public InstructorService(InstructorProfileRepo profiles, UserRepo users, PasswordEncoder encoder,
            OtpService otp) {
        this.profiles = profiles;
        this.users = users;
        this.encoder = encoder;
        this.otp = otp;
    }

    // ------------------------------------------------------------------
    // registration
public InstructorView register(InstructorRegisterRequest req) {
if (req == null) throw new InvalidRequestException("Registration details are required");
String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);
if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 120) {
throw new InvalidRequestException("Email must be valid");
}
String username = req.username() == null ? "" : req.username().trim();
if (username.isEmpty() || username.length() > 50 || username.contains(" ")) {
throw new InvalidRequestException("Username is required, without spaces, up to 50 characters");
}
if (req.password() == null || !req.password().matches(PASSWORD_POLICY)) {
throw new InvalidRequestException("Password must be 8 to 72 characters and include a letter and a digit");
}
if (req.mobileNumber() == null || !req.mobileNumber().matches("^[0-9]{10}$")) {
throw new InvalidRequestException("Mobile number must be 10 digits");
}
InstructorDetails details = checkDetails(new InstructorDetails(req.qualification(),
req.experienceYears(),
req.expertise(), req.bio(), req.profileLink()));
if (DatabaseOperationException.guard("checking the email", () -> users.existsByEmail(email))) {
throw new DuplicateResourceException("A user with this email already exists");
}
User user = new User();
user.setEmail(email);
user.setUsername(username);
user.setMobileNumber(req.mobileNumber());
user.setRole("INSTRUCTOR");
user.setPassword(encoder.encode(req.password()));
User saved;
try {
saved = users.save(user);
} catch (DataIntegrityViolationException e) {
throw new DuplicateResourceException("A user with this email already exists");
}
InstructorProfile p = new InstructorProfile();
p.setUserId(saved.getUserId());
apply(p, details);
p.setStatus(InstructorProfile.PENDING);
p.setSubmittedAt(LocalDateTime.now());
DatabaseOperationException.guardVoid("saving the application", () -> profiles.save(p));
otp.markPending(saved);
log.info("Instructor application {} submitted", saved.getUserId());
return view(saved, p);
}

    // ------------------------------------------------------------------ the
    // instructor's own application
    public InstructorView myProfile(String email) {
        User u = userByEmail(email);
        return view(u, profileOf(u.getUserId()));
    }

    /**
     * Fixing the application: allowed while PENDING, and a REJECTED application
     * goes back to PENDING.
     */
    public InstructorView updateMine(String email, InstructorDetails in) {
        User u = userByEmail(email);
        InstructorProfile p = profileOf(u.getUserId());
        if (InstructorProfile.APPROVED.equals(p.getStatus())) {
            throw new InvalidRequestException("An approved application can no longer be edited here");
        }
        apply(p, checkDetails(in));
        if (InstructorProfile.REJECTED.equals(p.getStatus())) {
            p.setStatus(InstructorProfile.PENDING);
            p.setRejectionReason(null);
            p.setSubmittedAt(LocalDateTime.now());
            p.setReviewedAt(null);
            p.setReviewedBy(null);
        }
        DatabaseOperationException.guardVoid("saving the application", () -> profiles.save(p));
        return view(u, p);
    }

    /**
     * Used before course and video management: only an APPROVED instructor may
     * continue.
     */
    public User requireApproved(String email) {
        User u = userByEmail(email);
        if (!"INSTRUCTOR".equals(u.getRole()))
            throw new AccessDeniedException("Instructors only");
        InstructorProfile p = profileOf(u.getUserId());
        if (!InstructorProfile.APPROVED.equals(p.getStatus())) {
            throw new AccessDeniedException("Your instructor account is not approved yet");
        }
        return u;
    }
    // ------------------------------------------------------------------ login rule
    // and the public status check

    /**
     * Login rule: an instructor may log in only after an administrator approved the
     * application. Called by
     * the login and
     * Google login right after the password (or Google token) was accepted. Other
     * roles pass untouched.
     */
    public void requireCanLogin(User user) {
        if (user == null || !"INSTRUCTOR".equals(user.getRole()))
            return;
        InstructorProfile p = DatabaseOperationException
                .guard("loading the application", () -> profiles.findByUserId(user.getUserId())).orElse(null);
        if (p == null || InstructorProfile.PENDING.equals(p.getStatus())) {
            throw new InstructorNotApprovedException(InstructorNotApprovedException.PENDING_MESSAGE);
        }
        if (InstructorProfile.REJECTED.equals(p.getStatus())) {
            throw new InstructorNotApprovedException(InstructorNotApprovedException.REJECTED_MESSAGE);
        }
    }

/**
* Public "where is my application?" check. The person types a username or an e-mail (an e-mail
contains "@").
* Usernames are not unique, so two instructors with the same name must use their e-mail. Customers and
admins are never
* found here, and the answer holds no phone number or bio.
*/
public ApplicationStatusView applicationStatus(String identifierRaw) {
String id = identifierRaw == null ? "" : identifierRaw.trim();
if (id.isEmpty() || id.length() > 120) throw new InvalidRequestException("Enter your username or email");
User u;
if (id.contains("@")) {
u = DatabaseOperationException.guard("loading a user", () ->
users.findByEmail(id.toLowerCase(Locale.ROOT)))
.filter(x -> "INSTRUCTOR".equals(x.getRole())).orElse(null);
} else {
List<User> same = DatabaseOperationException.guard("loading a user", () ->
users.findByUsernameIgnoreCaseAndRole(id, "INSTRUCTOR"));
if (same.size() > 1) throw new InvalidRequestException("More than one instructor uses this name. Please enter your e-mail instead.");
u = same.isEmpty() ? null : same.get(0);
}
InstructorProfile p = u == null ? null
: DatabaseOperationException.guard("loading the application", () ->
profiles.findByUserId(u.getUserId())).orElse(null);
if (u == null || p == null) throw new ResourceNotFoundException("No instructor application found for this username or e-mail.");
boolean checkOn = otp.isRequired();
boolean verified = !checkOn || !otp.isPending(u.getUserId());
return new ApplicationStatusView(u.getUsername(), maskEmail(u.getEmail()), p.getStatus(),
p.getSubmittedAt(), p.getReviewedAt(),
InstructorProfile.REJECTED.equals(p.getStatus()) ? p.getRejectionReason() : null, checkOn,
verified,
InstructorProfile.APPROVED.equals(p.getStatus()) && verified);
}

    /**
     * Fixing an application without being logged in (pending or rejected
     * instructors cannot log in): the
     * e-mail and password
     * prove who it is. The same rules as {@link #updateMine}: a rejected
     * application goes back to PENDING.
     */
    public InstructorView resubmit(ResubmitRequest in) {
        String email = in == null || in.email() == null ? "" : in.email().trim().toLowerCase(Locale.ROOT);
        User u = email.isEmpty() ? null
                : DatabaseOperationException.guard("loading a user", () -> users.findByEmail(email))
                        .filter(x -> "INSTRUCTOR".equals(x.getRole())).orElse(null);
        if (u == null || in.password() == null || !encoder.matches(in.password(), u.getPassword())) {
            throw new BadCredentialsException("Wrong e-mail or password");
        }
        return updateMine(u.getEmail(), new InstructorDetails(in.qualification(), in.experienceYears(),
                in.expertise(), in.bio(), in.profileLink()));
    }

    /** "karthik@gmail.com" becomes "k*****@gmail.com". */
    static String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        return (local.isEmpty() ? "" : local.substring(0, 1)) + "*****" + email.substring(at);
    }

    // ------------------------------------------------------------------ the
    // admin's review
    public List<InstructorView> list(String status) {
        List<InstructorProfile> rows;
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            rows = DatabaseOperationException.guard("listing applications",
                    profiles::findAllByOrderBySubmittedAtDesc);
        } else {
            String s = status.trim().toUpperCase(Locale.ROOT);
            if (!List.of("PENDING", "APPROVED", "REJECTED").contains(s)) {
                throw new InvalidRequestException("Status must be PENDING, APPROVED, REJECTED or ALL");
            }
            rows = DatabaseOperationException.guard("listing applications",
                    () -> profiles.findByStatusOrderBySubmittedAtDesc(s));
        }
        List<InstructorView> out = new ArrayList<>();
        for (InstructorProfile p : rows) {
            User u = DatabaseOperationException.guard("loading a user", () -> users.findById(p.getUserId()))
                    .orElse(null);
            if (u != null)
                out.add(view(u, p));
        }
        return out;
    }

public InstructorView approve(Long userId, String adminEmail) {
User u = userById(userId);
InstructorProfile p = profileOf(userId);
if (InstructorProfile.APPROVED.equals(p.getStatus())) throw new InvalidRequestException("This instructor is already approved");
p.setStatus(InstructorProfile.APPROVED);
p.setRejectionReason(null);
p.setReviewedAt(LocalDateTime.now());
p.setReviewedBy(adminEmail);
DatabaseOperationException.guardVoid("saving the decision", () -> profiles.save(p));
log.info("Instructor {} approved by {}", userId, adminEmail);
return view(u, p);
}

public InstructorView reject(Long userId, String reasonRaw, String adminEmail) {
String reason = reasonRaw == null ? "" : reasonRaw.replaceAll("\\p{Cntrl}", " ").trim();
if (reason.length() < MIN_REASON || reason.length() > MAX_REASON) { throw new InvalidRequestException("Give a reason of " + MIN_REASON + " to " + MAX_REASON + " characters");
}
User u = userById(userId);
InstructorProfile p = profileOf(userId);
if (!InstructorProfile.PENDING.equals(p.getStatus())) {
throw new InvalidRequestException("Only a pending application can be rejected");
}
p.setStatus(InstructorProfile.REJECTED);
p.setRejectionReason(reason);
p.setReviewedAt(LocalDateTime.now());
p.setReviewedBy(adminEmail);
DatabaseOperationException.guardVoid("saving the decision", () -> profiles.save(p));
log.info("Instructor {} rejected by {}", userId, adminEmail);
return view(u, p);
}

    // ------------------------------------------------------------------ helpers
    private User userByEmail(String email) {
        return DatabaseOperationException
                .guard("loading a user",
                        () -> users.findByEmail(email == null ? "" : email.trim().toLowerCase(Locale.ROOT)))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User userById(Long id) {
        return DatabaseOperationException.guard("loading a user", () -> users.findById(id))
                .filter(x -> "INSTRUCTOR".equals(x.getRole()))
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
    }

    private InstructorProfile profileOf(Long userId) {
        return DatabaseOperationException.guard("loading the application", () -> profiles.findByUserId(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Instructor application not found"));
    }

    private static void apply(InstructorProfile p, InstructorDetails d) {
        p.setQualification(d.qualification());
        p.setExperienceYears(d.experienceYears());
        p.setExpertise(d.expertise());
        p.setBio(d.bio());
        p.setProfileLink(d.profileLink());
    }

/** Checks and trims the application fields. Returns a clean copy. */
static InstructorDetails checkDetails(InstructorDetails d) {
if (d == null) throw new InvalidRequestException("Application details are required");
String q = clean(d.qualification());
String e = clean(d.expertise());
String b = clean(d.bio());
String link = clean(d.profileLink());
if (q.length() < 2 || q.length() > 100) throw new InvalidRequestException("Qualification is required (2 to 100 characters)");
if (d.experienceYears() == null || d.experienceYears() < 0 || d.experienceYears() > 60) {
throw new InvalidRequestException("Years of experience must be between 0 and 60");
}
if (e.length() < 2 || e.length() > 150) throw new InvalidRequestException("Area of expertise is required (2 to 150 characters)");
if (b.length() < 20 || b.length() > 1000) throw new InvalidRequestException("Short bio must be 20 to 1000 characters");
if (!link.isEmpty() && (!link.matches("^https?://\\S+$") || link.length() > 300)) {
throw new InvalidRequestException("Profile link must start with http:// or https://");
}
return new InstructorDetails(q, d.experienceYears(), e, b, link);
}

    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("\\p{Cntrl}", " ").trim();
    }

    static InstructorView view(User u, InstructorProfile p) {
        return new InstructorView(u.getUserId(), u.getUsername(), u.getEmail(), u.getMobileNumber(),
                p.getQualification(),
                p.getExperienceYears(), p.getExpertise(), p.getBio(), p.getProfileLink(), p.getStatus(),
                p.getRejectionReason(), p.getSubmittedAt(), p.getReviewedAt(), p.getReviewedBy());
    }
}
