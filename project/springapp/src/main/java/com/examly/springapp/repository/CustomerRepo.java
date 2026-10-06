package com.examly.springapp.repository;

import com.examly.springapp.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for customer profiles.
 *
 * @author Meruva Lokesh
 */
@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser_UserId(Long userId);
}
