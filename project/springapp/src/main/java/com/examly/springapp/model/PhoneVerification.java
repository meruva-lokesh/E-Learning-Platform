package com.examly.springapp.model;

import jakarta.persistence.*;

/**
 * Phone verification state of one account (customer or instructor). A row with verified = false means
 * "this account must verify its mobile number before it can log in". Accounts without a row (all accounts
 * created before OTP existed, admins, Google logins) count as verified. The real code is never stored,
 * only a salted hash of it.
 */
@Entity
@Table(name = "phone_verifications")
public class PhoneVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private boolean verified;

    @Column(length = 64)
    private String codeHash;

    @Column(length = 32)
    private String salt;

    /** When the current code stops working (milliseconds since 1970). */
    private long expiresAtMs;

    /** Wrong tries on the current code. */
    private int attempts;

    private long lastSentAtMs;
    private long verifiedAtMs;

    public PhoneVerification() {}

    public PhoneVerification(Long userId, boolean verified) {
        this.userId = userId;
        this.verified = verified;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public long getExpiresAtMs() { return expiresAtMs; }
    public void setExpiresAtMs(long expiresAtMs) { this.expiresAtMs = expiresAtMs; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public long getLastSentAtMs() { return lastSentAtMs; }
    public void setLastSentAtMs(long lastSentAtMs) { this.lastSentAtMs = lastSentAtMs; }
    public long getVerifiedAtMs() { return verifiedAtMs; }
    public void setVerifiedAtMs(long verifiedAtMs) { this.verifiedAtMs = verifiedAtMs; }
}
