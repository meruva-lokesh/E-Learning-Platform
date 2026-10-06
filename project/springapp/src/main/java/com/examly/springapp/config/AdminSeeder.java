package com.examly.springapp.config;

import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import com.examly.springapp.service.UserService;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Makes sure the main admin from application.properties exists (checked by email at every start).
 * The main admin is the only one who can add or remove other admins. The password comes from configuration
 * (ADMIN_PASSWORD) and is never logged. Set app.admin.reset-password=true for one start to put the configured
 * password back if it was forgotten.
 *
 * @author Suriya
 */
@Component
public class AdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepo userRepo;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String username;
    private final String mobile;
    private final boolean resetPassword;

    public AdminSeeder(UserRepo userRepo, UserService userService, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.password}") String password,
                       @Value("${app.admin.username:Administrator}") String username,
                       @Value("${app.admin.mobile:9999999999}") String mobile,
                       @Value("${app.admin.reset-password:false}") boolean resetPassword) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.username = username;
        this.mobile = mobile;
        this.resetPassword = resetPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<User> existing = userRepo.findByEmail(email);
        if (existing.isEmpty()) {
            userService.createAdmin(new User(email, password, username, mobile, "ADMIN"));
            log.info("Main admin created with email {}", email);
            return;
        }
        User main = existing.get();
        if (!"ADMIN".equals(main.getRole())) {
            log.warn("The configured main admin email {} belongs to a non-admin account; change app.admin.email", email);
            return;
        }
        if (resetPassword) {
            main.setPassword(passwordEncoder.encode(password));
            userRepo.save(main);
            log.warn("Main admin password was reset from the configuration (turn app.admin.reset-password off again)");
        } else {
            log.info("Main admin {} already exists", email);
        }
    }
}