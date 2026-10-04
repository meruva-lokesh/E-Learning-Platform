package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.LoginDTO;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.repository.UserRepo;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login rules: unique email, password policy, customer-only public sign-up, admin management and timing-safe credential checks.
 * Passwords and tokens are never written to the logs.
 *
 * @author Suriya
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String PASSWORD_POLICY = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepo refreshTokenRepo;
    /** The main admin from application.properties; this account can never be removed. */
    private final String mainAdminEmail;
    /** Compared against when the email is unknown so response time does not reveal which emails exist. */
    private final String dummyHash;

    public UserServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder, RefreshTokenRepo refreshTokenRepo,
                           @Value("${app.admin.email}") String mainAdminEmail) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepo = refreshTokenRepo;
        this.mainAdminEmail = mainAdminEmail.trim().toLowerCase(Locale.ROOT);
        this.dummyHash = passwordEncoder.encode("not-a-real-password-1");
    }

    @Override
    public User register(User user) {
        log.trace("register entered");
        String requested = user.getRole() == null ? "" : user.getRole().trim().toUpperCase(Locale.ROOT);
        // Business rule: nobody can sign themselves up as an admin. Admins are created by the seeder or by another admin.
        if (requested.equals("ADMIN")) {
            log.warn("Public registration refused: ADMIN role requested");
            throw new AccessDeniedException("Admin accounts can only be created by an administrator");
        }
        return create(user, "CUSTOMER");
    }

    @Override
    public User createAdmin(User user) {
        log.trace("createAdmin entered");
        return create(user, "ADMIN");
    }

    @Override
    public List<User> listAdmins() {
        log.trace("listAdmins entered");
        return DatabaseOperationException.guard("listing admins", () -> userRepo.findByRole("ADMIN"));
    }

    @Override
    @Transactional
    public void deleteAdmin(Long userId, String currentEmail) {
        log.trace("deleteAdmin entered");
        User target = DatabaseOperationException.guard("loading a user", () -> userRepo.findById(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!"ADMIN".equals(target.getRole())) {
            throw new InvalidRequestException("Only admin accounts can be removed here");
        }
        // Business rules: the main admin is permanent, you cannot lock yourself out, and one admin always remains.
        if (target.getEmail().equalsIgnoreCase(mainAdminEmail)) {
            throw new InvalidRequestException("The main admin cannot be removed");
        }
        if (target.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new InvalidRequestException("You cannot delete your own account");
        }
        if (userRepo.countByRole("ADMIN") <= 1) {
            throw new InvalidRequestException("At least one admin must remain");
        }
        try {
            refreshTokenRepo.deleteByUser_UserId(userId);   // end the removed admin's sessions first
            userRepo.delete(target);
            log.info("Admin {} removed", userId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while removing an admin", e);
        }
    }

    /** Shared by sign-up and admin creation: password policy, unique email, BCrypt, save. */
    private User create(User user, String role) {
        if (user.getPassword() == null || !user.getPassword().matches(PASSWORD_POLICY)) {
            throw new InvalidRequestException("Password must be 8 to 72 characters and include a letter and a digit");
        }
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (DatabaseOperationException.guard("checking the email", () -> userRepo.existsByEmail(email))) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        user.setUserId(null);
        user.setEmail(email);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            User saved = userRepo.save(user);
            log.info("User {} created with role {}", saved.getUserId(), role);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // two requests used the same email at the same moment (unique constraint)
            throw new DuplicateResourceException("A user with this email already exists");
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while creating a user", e);
        }
    }

    @Override
    public User authenticate(LoginDTO login) {
        log.trace("authenticate entered");
        String email = login.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(email)).orElse(null);
        boolean ok = passwordEncoder.matches(login.getPassword(), user == null ? dummyHash : user.getPassword());
        if (user == null || !ok) {
            log.warn("Login failed: wrong email or password");   // never say which of the two was wrong
            throw new BadCredentialsException("Invalid credentials");
        }
        log.info("User {} logged in", user.getUserId());
        return user;
    }

    @Override
    public User getByEmail(String email) {
        log.trace("getByEmail entered");
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        return DatabaseOperationException.guard("loading a user", () -> userRepo.findByEmail(normalised))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}