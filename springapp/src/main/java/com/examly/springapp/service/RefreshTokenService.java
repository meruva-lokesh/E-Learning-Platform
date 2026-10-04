package com.examly.springapp.service;

import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.RefreshTokenRepo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque refresh tokens. The raw token only ever lives in the user's HttpOnly cookie;
 * the database keeps its SHA-256 hash. Each token works once (rotation), and replaying
 * an old token revokes every session of that user. Raw tokens are never logged.
 *
 * @author Suriya
 */
@Service
public class RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    /** A token revoked this recently is treated as a harmless double click / second tab, not as theft. */
    private static final long GRACE_SECONDS = 10;

    private final RefreshTokenRepo repo;
    private final long expirationDays;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepo repo,
                               @Value("${jwt.refresh-expiration-days:7}") long expirationDays) {
        this.repo = repo;
        this.expirationDays = expirationDays;
    }

    public String issue(User user) {
        log.trace("issue entered for userId={}", user.getUserId());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(raw));
        token.setUser(user);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS));
        repo.save(token);
        log.debug("Refresh token issued for user {}", user.getUserId());
        return raw;
    }

    /** Validates the token, marks it used and returns its owner. */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public User consume(String raw) {
        log.trace("consume entered");
        if (raw == null || raw.isBlank()) {
            log.debug("Refresh refused: no cookie");
            throw new BadCredentialsException("Missing refresh token");
        }
        RefreshToken token = repo.findByTokenHash(hash(raw))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (token.isRevoked()) {
            boolean recently = token.getRevokedAt() != null
                    && token.getRevokedAt().isAfter(Instant.now().minusSeconds(GRACE_SECONDS));
            if (!recently) {
                // replay of an old token: assume it was stolen and end every session of that user
                log.warn("Refresh token reuse detected for user {}: revoking all sessions", token.getUser().getUserId());
                revokeAll(token.getUser().getUserId());
            }
            throw new BadCredentialsException("Refresh token already used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        repo.save(token);
        log.info("Access token refreshed for user {}", token.getUser().getUserId());
        return token.getUser();
    }

    @Transactional
    public void revoke(String raw) {
        log.trace("revoke entered");
        if (raw == null || raw.isBlank()) return;
        repo.findByTokenHash(hash(raw)).ifPresent(t -> {
            if (!t.isRevoked()) {
                t.setRevoked(true);
                t.setRevokedAt(Instant.now());
                repo.save(t);
                log.info("Refresh token revoked (logout) for user {}", t.getUser().getUserId());
            }
        });
    }

    @Transactional
    public void revokeAll(Long userId) {
        for (RefreshToken t : repo.findByUser_UserIdAndRevokedFalse(userId)) {
            t.setRevoked(true);
            t.setRevokedAt(Instant.now());
            repo.save(t);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        long removed = repo.deleteByExpiresAtBefore(Instant.now());
        log.debug("Purged {} expired refresh token(s)", removed);
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
