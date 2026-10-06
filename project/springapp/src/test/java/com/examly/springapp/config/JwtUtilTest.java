package com.examly.springapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for access-token creation and validation, including the userId and username claims.
 *
 * @author Suriya
 */
class JwtUtilTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void generatedTokenIsValidAndCarriesEmailAndRole() {
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        String token = jwt.generateToken("a@b.com", "ADMIN", 7L, "alice");
        assertTrue(jwt.isValid(token));
        assertEquals("a@b.com", jwt.extractEmail(token));
        assertEquals("ADMIN", jwt.extractRole(token));
    }

    @Test
    void tokenCarriesUserIdAndUsernameClaims() {
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        String token = jwt.generateToken("a@b.com", "CUSTOMER", 42L, "alice");
        assertEquals(Long.valueOf(42L), jwt.extractUserId(token));
        assertEquals("alice", jwt.extractUsername(token));
        // the same claims are visible to a client that only decodes the token (jwt-decode)
        Claims raw = Jwts.parserBuilder().setSigningKey(SECRET.getBytes(StandardCharsets.UTF_8)).build()
                .parseClaimsJws(token).getBody();
        assertEquals("a@b.com", raw.getSubject());
        assertEquals("CUSTOMER", raw.get("role", String.class));
        assertEquals(42, ((Number) raw.get("userId")).intValue());
        assertEquals("alice", raw.get("username", String.class));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil jwt = new JwtUtil(SECRET, -1000);
        assertFalse(jwt.isValid(jwt.generateToken("a@b.com", "CUSTOMER", 8L, "bob")));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        String token = jwt.generateToken("a@b.com", "CUSTOMER", 8L, "bob");
        assertFalse(jwt.isValid(token.substring(0, token.length() - 3) + "abc"));
        assertFalse(jwt.isValid("not-a-token"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtUtil other = new JwtUtil("another-secret-another-secret-another-1", 60_000);
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        assertFalse(jwt.isValid(other.generateToken("a@b.com", "ADMIN", 7L, "alice")));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("short", 60_000));
    }
}
