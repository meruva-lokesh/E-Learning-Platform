package com.examly.springapp.service;

import com.examly.springapp.config.RateLimiter;
import com.examly.springapp.dto.OtpDtos.*;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.PhoneNotVerifiedException;
import com.examly.springapp.exception.RateLimitExceededException;
import com.examly.springapp.model.PhoneVerification;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.PhoneVerificationRepo;
import com.examly.springapp.repository.UserRepo;
import com.examly.springapp.sms.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Mobile number verification with a one-time password (OTP).
 * <ul>
 * <li>A 6 digit code is made with SecureRandom, stored only as a salted SHA-256 hash, valid 5 minutes.</li>
 * <li>5 wrong tries kill the code; a new code can be requested after 30 seconds, at most 5 per hour.</li>
 * <li>Answers never reveal whether an email exists or is waiting for verification.</li>
 * <li>When otp.required=false (the default) nothing is enforced, so existing flows and tests are unchanged.</li>
 * </ul>
 */
@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    static final long CODE_TTL_MS = 5 * 60_000L;
    static final int MAX_ATTEMPTS = 5;
    static final int RESEND_SECONDS = 30;
    private static final long HOUR_MS = 60 * 60_000L;
    static final String GENERIC_SEND = "If this account is waiting for verification, a code has been sent to its mobile number.";

    private final PhoneVerificationRepo repo;
    private final UserRepo userRepo;
    private final SmsSender sms;
    private final RateLimiter limiter;
    private final boolean required;
    private final boolean exposeDevCode;
    private final String countryCode;
    private final LongSupplier clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public OtpService(PhoneVerificationRepo repo, UserRepo userRepo, SmsSender sms, RateLimiter limiter, Environment env,
                      @Value("${otp.required:false}") boolean required,
                      @Value("${otp.dev-expose-code:false}") boolean exposeDevCode,
                      @Value("${otp.country-code:+91}") String countryCode) {
        this(repo, userRepo, sms, limiter, required, exposeDevCode, countryCode, System::currentTimeMillis);
        if (exposeDevCode && env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("Refusing to start: otp.dev-expose-code must be false in the prod profile");
        }
    }

    OtpService(PhoneVerificationRepo repo, UserRepo userRepo, SmsSender sms, RateLimiter limiter, boolean required,
               boolean exposeDevCode, String countryCode, LongSupplier clock) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.sms = sms;
        this.limiter = limiter;
        this.required = required;
        this.exposeDevCode = exposeDevCode;
        this.countryCode = countryCode;
        this.clock = clock;
    }

    public boolean isRequired() {
        return required;
    }

    // ------------------------------------------------------------------ hooks used by registration and login

    /** Called right after a customer or instructor registers: from now on the account must verify its number. */
    public void markPending(User user) {
        if (!required || user == null || user.getUserId() == null) return;
        PhoneVerification row = DatabaseOperationException.guard("loading phone verification",
                () -> repo.findByUserId(user.getUserId())).orElseGet(() -> new PhoneVerification(user.getUserId(), false));
        row.setVerified(false);
        DatabaseOperationException.guardVoid("saving phone verification", () -> repo.save(row));
    }

    /** Called by login after the password was accepted: blocks accounts that still wait for verification. */
    public void requireVerified(User user) {
        if (!required || user == null) return;
        if (isPending(user.getUserId())) throw new PhoneNotVerifiedException();
    }

    /** True only when a row exists and says "not verified yet". */
    public boolean isPending(Long userId) {
        return DatabaseOperationException.guard("checking phone verification",
                () -> repo.findByUserId(userId)).map(r -> !r.isVerified()).orElse(false);
    }

    // ------------------------------------------------------------------ send

    public OtpSendResponse send(String emailRaw, String clientIp) {
        if (!required) {
            return new OtpSendResponse(false, "Mobile verification is not switched on.", 0, null);
        }
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || email.length() > 120) {
            throw new InvalidRequestException("Email is required");
        }
        // limits apply to every email, existing or not, so the answers do not reveal which emails exist
        guardRate("otp:ip:" + clientIp, 20, HOUR_MS, "Too many code requests from this device. Try again later.");
        guardRate("otp:gap:" + email, 1, RESEND_SECONDS * 1000L, "Please wait before asking for another code.");
        guardRate("otp:hour:" + email, 5, HOUR_MS, "Too many codes requested for this account. Try again in an hour.");

        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).orElse(null);
        PhoneVerification row = user == null ? null
                : DatabaseOperationException.guard("loading phone verification", () -> repo.findByUserId(user.getUserId())).orElse(null);
        String devCode = null;
        if (user != null && row != null && !row.isVerified()) {
            String code = newCode();
            String salt = newSalt();
            row.setSalt(salt);
            row.setCodeHash(hash(salt, code));
            row.setAttempts(0);
            row.setLastSentAtMs(clock.getAsLong());
            row.setExpiresAtMs(clock.getAsLong() + CODE_TTL_MS);
            DatabaseOperationException.guardVoid("saving the code", () -> repo.save(row));
            sms.send(toE164(user.getMobileNumber()),
                    "Your E-Learning Platform verification code is " + code + ". It expires in 5 minutes. Do not share it.");
            log.info("OTP sent for user {}", user.getUserId());
            if (exposeDevCode) devCode = code;
        }
        return new OtpSendResponse(true, GENERIC_SEND, RESEND_SECONDS, devCode);
    }

    // ------------------------------------------------------------------ verify

    public OtpVerifyResponse verify(String emailRaw, String code, String clientIp) {
        if (!required) {
            return new OtpVerifyResponse(true, "Mobile verification is not switched on.");
        }
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || code == null || !code.matches("^[0-9]{6}$")) {
            throw new InvalidRequestException("Enter the 6 digit code");
        }
        guardRate("otp:verify-ip:" + clientIp, 30, HOUR_MS, "Too many attempts from this device. Try again later.");

        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).orElse(null);
        PhoneVerification row = user == null ? null
                : DatabaseOperationException.guard("loading phone verification", () -> repo.findByUserId(user.getUserId())).orElse(null);
        if (row == null || row.isVerified() || row.getCodeHash() == null
                || clock.getAsLong() > row.getExpiresAtMs() || row.getAttempts() >= MAX_ATTEMPTS) {
            throw new InvalidRequestException("This code is wrong or has expired. Request a new code.");
        }
        boolean match = MessageDigest.isEqual(
                hash(row.getSalt(), code).getBytes(StandardCharsets.UTF_8), row.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!match) {
            row.setAttempts(row.getAttempts() + 1);
            DatabaseOperationException.guardVoid("saving the attempt", () -> repo.save(row));
            int left = MAX_ATTEMPTS - row.getAttempts();
            throw new InvalidRequestException(left > 0
                    ? "Wrong code. " + left + (left == 1 ? " try" : " tries") + " left."
                    : "Too many wrong codes. Request a new code.");
        }
        row.setVerified(true);
        row.setCodeHash(null);
        row.setSalt(null);
        row.setAttempts(0);
        row.setVerifiedAtMs(clock.getAsLong());
        DatabaseOperationException.guardVoid("saving verification", () -> repo.save(row));
        log.info("Mobile number verified for user {}", user.getUserId());
        return new OtpVerifyResponse(true, "Your mobile number is verified. You can log in now.");
    }

    // ------------------------------------------------------------------ helpers

    private void guardRate(String key, int limit, long windowMs, String message) {
        if (!limiter.tryAcquire(key, limit, windowMs)) {
            throw new RateLimitExceededException(message, limiter.retryAfterSeconds(key, windowMs));
        }
    }

    String toE164(String mobile) {
        String digits = mobile == null ? "" : mobile.replaceAll("[^0-9]", "");
        return countryCode + digits;
    }

    private String newCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private String newSalt() {
        byte[] b = new byte[12];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String hash(String salt, String code) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
