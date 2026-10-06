package com.examly.springapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for refresh-token hashing.
 *
 * @author Suriya
 */
class RefreshTokenServiceTest {

    @Test
    void hashIsStableAndNeverEqualsTheRawToken() {
        String h1 = RefreshTokenService.hash("raw-token");
        String h2 = RefreshTokenService.hash("raw-token");
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
        assertNotEquals("raw-token", h1);
        assertNotEquals(h1, RefreshTokenService.hash("other-token"));
    }
}
