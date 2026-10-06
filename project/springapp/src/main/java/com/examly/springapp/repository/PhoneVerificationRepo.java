package com.examly.springapp.repository;

import com.examly.springapp.model.PhoneVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for phone verification rows. */
@Repository
public interface PhoneVerificationRepo extends JpaRepository<PhoneVerification, Long> {
    Optional<PhoneVerification> findByUserId(Long userId);
}
