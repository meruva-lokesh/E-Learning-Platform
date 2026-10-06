package com.examly.springapp.service;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.UserRepo;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Who is calling, and may they touch this data? Controllers use it so the identity always comes
 * from the verified JWT and never from ids the client sends (this prevents reading other people's data).
 *
 * @author Suriya
 */
@Service
public class AccessService {
    private final UserRepo userRepo;
    private final CustomerRepo customerRepo;

    public AccessService(UserRepo userRepo, CustomerRepo customerRepo) {
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
    }

    private Authentication auth() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new AccessDeniedException("Not authenticated");
        }
        return a;
    }

    public String currentEmail() {
        return auth().getName();
    }

    public boolean isAdmin() {
        return auth().getAuthorities().stream().anyMatch(g -> "ADMIN".equals(g.getAuthority()));
    }

    public User currentUser() {
        return userRepo.findByEmail(currentEmail())
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }

    public Customer currentCustomer() {
        Long userId = currentUser().getUserId();
        return customerRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer details not found. Please complete your profile first."));
    }

    /** Admins may access any customer; customers only their own. */
    public void requireCustomerAccess(Long customerId) {
        if (isAdmin()) return;
        if (customerId == null || !currentCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You can only access your own data");
        }
    }

    /** Admins may access any user; others only themselves. */
    public void requireUserAccess(Long userId) {
        if (isAdmin()) return;
        if (userId == null || !currentUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only access your own data");
        }
    }
}
