package com.examly.springapp.repository;

import com.examly.springapp.model.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for refresh tokens.
 *
 * @author Suriya
 */
@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUser_UserIdAndRevokedFalse(Long userId);
    long deleteByExpiresAtBefore(Instant cutoff);
    void deleteByUser_UserId(Long userId);
}