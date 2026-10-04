package com.examly.springapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Stops the app from starting in the prod profile with unsafe development defaults.
 *
 * @author Suriya
 */
@Component
public class ProdSafetyCheck {
    public ProdSafetyCheck(Environment env,
                           @Value("${jwt.secret}") String secret,
                           @Value("${app.admin.password}") String adminPassword) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            if (secret.startsWith("dev-only") || secret.length() < 32) {
                throw new IllegalStateException("Refusing to start: set a strong JWT_SECRET (32+ chars) for the prod profile");
            }
            if (adminPassword.isBlank() || adminPassword.equals("Admin@12345")) {
                throw new IllegalStateException("Refusing to start: set ADMIN_PASSWORD (not the default) for the prod profile");
            }
        }
    }
}