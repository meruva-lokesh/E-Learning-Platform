package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Server-side record of an issued refresh token. Only the SHA-256 hash of the token is stored.
 *
 * @author Suriya
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;

    /** No-arg constructor required by JPA. */
    public RefreshToken() {}

    /** Convenience constructor for a fresh, not yet revoked token; chains to the all-fields constructor. */
    public RefreshToken(String tokenHash, User user, Instant createdAt, Instant expiresAt) {
        this(null, tokenHash, user, createdAt, expiresAt, false, null);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public RefreshToken(Long id, String tokenHash, User user, Instant createdAt, Instant expiresAt,
                        boolean revoked, Instant revokedAt) {
        this();
        this.id = id;
        this.tokenHash = tokenHash;
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
