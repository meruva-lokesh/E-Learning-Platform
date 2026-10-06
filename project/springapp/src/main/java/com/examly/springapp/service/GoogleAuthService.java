package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

/**
 * "Login with Google" (OAuth 2.0 / OpenID Connect).
 * <p>
 * The browser gives us a Google ID token. This class proves the token is real by checking
 * Google's signature (public keys from Google), the issuer, the audience (our own client id)
 * and the expiry. Then it finds the user with that e-mail, or creates a CUSTOMER account.
 * The caller (GoogleAuthController) then issues the same JWT and refresh cookie as /api/login.
 * <p>
 * Nothing here is a secret: the client id is public. No password or Google token is logged.
 */
@Service
public class GoogleAuthService {
    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    /** The users table requires a 10 digit mobile number; Google does not give one. The user can change it later. */
    private static final String PLACEHOLDER_MOBILE = "0000000000";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final NimbusJwtDecoder decoder;   // null when google.client-id is not set

    public GoogleAuthService(UserRepo userRepo, PasswordEncoder passwordEncoder,
                             @Value("${google.client-id:}") String clientId) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.decoder = clientId == null || clientId.isBlank() ? null : buildDecoder(clientId.trim());
        if (this.decoder == null) {
            log.warn("google.client-id is not set: Login with Google is switched off");
        }
    }

    private static NimbusJwtDecoder buildDecoder(String clientId) {
        NimbusJwtDecoder d = NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS_URL).build();
        OAuth2TokenValidator<Jwt> issuerCheck = jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong token issuer", null));
        OAuth2TokenValidator<Jwt> audienceCheck = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token is for another app", null));
        // createDefault() checks the expiry time; the other two checks are ours
        d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), issuerCheck, audienceCheck));
        return d;
    }

    /**
     * "Log in with Google": the Google e-mail must already belong to a registered account. A person who never registered gets
     * 404 "No account found ... register first" and nothing is stored.
     */
    public User loginWithGoogle(String credential) {
        log.trace("loginWithGoogle entered");
        Jwt jwt = verify(credential);
        String email = verifiedEmail(jwt);
        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).orElse(null);
        if (user == null) {
            log.warn("Google login refused: no account for this Google e-mail");
            throw new ResourceNotFoundException("No account found for this Google e-mail. Please register first.");
        }
        refuseAdmin(user);
        log.info("User {} logged in with Google", user.getUserId());
        return user;
    }

    /**
     * "Sign up with Google": creates a CUSTOMER account from the verified Google e-mail and name and stores it in the users table.
     * If the e-mail is already registered the answer is 409 "already registered, log in instead" and nothing changes.
     */
    public User registerWithGoogle(String credential) {
        log.trace("registerWithGoogle entered");
        Jwt jwt = verify(credential);
        String email = verifiedEmail(jwt);
        if (DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).isPresent()) {
            log.warn("Google sign-up refused: the e-mail is already registered");
            throw new DuplicateResourceException("An account with this email already exists. Please log in instead.");
        }
        return createCustomer(email, jwt.getClaimAsString("name"));
    }

    /** Checks the Google signature, issuer, audience and expiry. */
    private Jwt verify(String credential) {
        if (decoder == null) {
            throw new InvalidRequestException("Login with Google is not set up on this server");
        }
        try {
            return decoder.decode(credential);
        } catch (JwtException e) {
            log.warn("Google login refused: token could not be verified");
            throw new BadCredentialsException("Invalid Google token");
        }
    }

    private static String verifiedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            log.warn("Google login refused: e-mail missing or not verified by Google");
            throw new BadCredentialsException("Google account e-mail is not verified");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Business rule: admin accounts keep using the password login only (customers and instructors may use Google). */
    private static void refuseAdmin(User user) {
        if ("ADMIN".equals(user.getRole())) {
            log.warn("Google login refused for an admin account");
            throw new AccessDeniedException("Admin accounts must log in with their password");
        }
    }

    private User createCustomer(String email, String googleName) {
        String name = googleName == null || googleName.isBlank() ? email.substring(0, email.indexOf('@')) : googleName.trim();
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        // nobody knows this password, so the account can only be entered through Google (or "forgot password" later)
        User user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), name, PLACEHOLDER_MOBILE, "CUSTOMER");
        try {
            User saved = userRepo.save(user);
            log.info("User {} created from Google login", saved.getUserId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // two Google logins for a new e-mail arrived at the same moment: use the one that won
            return userRepo.findByEmail(email)
                    .orElseThrow(() -> new DuplicateResourceException("A user with this email already exists"));
        }
    }
}
