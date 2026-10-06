package com.examly.springapp.service;

import com.examly.springapp.config.RateLimiter;
import com.examly.springapp.dto.ResetDtos.ForgotResponse;
import com.examly.springapp.dto.ResetDtos.ResetResponse;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.RateLimitExceededException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Forgot password" without e-mail: the person types the e-mail, then a new password, and the password is changed at once.
 * <p>
 * <b>Security note.</b> Nothing proves that the person owns the e-mail, so anyone who knows an address can change that
 * account's password. To limit the damage this service (1) never resets an ADMIN account, (2) limits attempts per address and
 * per device, (3) revokes every refresh token after a reset, and (4) can ask for the account's registered mobile number
 * as a second detail when {@code reset.require-mobile=true}. For a real product use the e-mailed link instead.
 */
@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long HOUR_MS = 3_600_000L;
    private static final String PASSWORD_POLICY = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    /** The mobile number stored for accounts created with Google sign-up; it is not a real secret, so it cannot be used as proof. */
    static final String GOOGLE_PLACEHOLDER_MOBILE = "0000000000";

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshTokens;
    private final RateLimiter limiter;
    private final boolean requireMobile;

    @Autowired
    public PasswordResetService(UserRepo userRepo, PasswordEncoder encoder, RefreshTokenService refreshTokens, RateLimiter limiter,
                                @Value("${reset.require-mobile:false}") boolean requireMobile) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.limiter = limiter;
        this.requireMobile = requireMobile;
    }

    /** Step 1: checks the e-mail looks right and tells the page what step 2 needs. It does not reveal whether an account exists. */
    public ForgotResponse start(String emailRaw, String clientIp) {
        guardRate("reset:start-ip:" + clientIp, 60, HOUR_MS, "Too many requests from this device. Try again later.");
        cleanEmail(emailRaw);
        return new ForgotResponse(requireMobile);
    }

    /** Step 2: changes the password. */
    @Transactional
    public ResetResponse reset(String emailRaw, String newPassword, String mobileRaw, String clientIp) {
        String email = cleanEmail(emailRaw);
        guardRate("reset:ip:" + clientIp, 20, HOUR_MS, "Too many attempts from this device. Try again later.");
        guardRate("reset:email:" + email, 10, HOUR_MS, "Too many attempts for this account. Try again in an hour.");
        if (newPassword == null || !newPassword.matches(PASSWORD_POLICY)) {
            throw new InvalidRequestException("Password must be 8 to 72 characters with a letter and a number");
        }
        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).orElse(null);
        if (user == null) {
            throw new ResourceNotFoundException("No account found for this e-mail. Please register first.");
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new InvalidRequestException("Admin passwords cannot be reset here. Ask the platform owner.");
        }
        if (requireMobile) {
            String stored = user.getMobileNumber() == null ? "" : user.getMobileNumber().trim();
            if (GOOGLE_PLACEHOLDER_MOBILE.equals(stored)) {
                throw new InvalidRequestException("This account was created with Google. Use Continue with Google to log in.");
            }
            String given = mobileRaw == null ? "" : mobileRaw.replaceAll("\\D", "");
            if (!given.equals(stored)) {
                throw new InvalidRequestException("The mobile number does not match this account.");
            }
        }
        user.setPassword(encoder.encode(newPassword));
        DatabaseOperationException.guardVoid("saving the new password", () -> userRepo.save(user));
        refreshTokens.revokeAll(user.getUserId());   // every device must log in again with the new password
        log.info("Password reset completed for user {}", user.getUserId());
        return new ResetResponse("Your password has been changed. You can log in now.");
    }

    private static String cleanEmail(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || email.length() > 120 || !email.contains("@")) {
            throw new InvalidRequestException("Enter a valid e-mail address");
        }
        return email;
    }

    private void guardRate(String key, int limit, long windowMs, String message) {
        if (!limiter.tryAcquire(key, limit, windowMs)) {
            throw new RateLimitExceededException(message, limiter.retryAfterSeconds(key, windowMs));
        }
    }
}
