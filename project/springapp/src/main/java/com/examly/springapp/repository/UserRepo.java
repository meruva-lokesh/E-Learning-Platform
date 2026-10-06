package com.examly.springapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.examly.springapp.model.User;

/**
 * Data access for user accounts.
 *
 * @author Suriya
 */
@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Used by the instructor status check: usernames are not unique, so this
     * returns a list.
     */
    List<User> findByUsernameIgnoreCaseAndRole(String username, String role);

    List<User> findByRole(String role);

    long countByRole(String role);
}