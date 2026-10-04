package com.examly.springapp.service;

import com.examly.springapp.model.LoginDTO;
import com.examly.springapp.model.User;
import java.util.List;

/**
 * Business operations on user accounts: registration and credential checks.
 *
 * @author Suriya
 */
public interface UserService {
    /** Public sign-up. Always creates a CUSTOMER; asking for the ADMIN role is refused. */
    User register(User user);
    /** Admin-only: creates another admin account. */
    User createAdmin(User user);
    /** Admin-only: lists the admin accounts. */
    List<User> listAdmins();
    /** Admin-only: removes an admin (never yourself, never the last one). */
    void deleteAdmin(Long userId, String currentEmail);
    User authenticate(LoginDTO login);
    User getByEmail(String email);
}