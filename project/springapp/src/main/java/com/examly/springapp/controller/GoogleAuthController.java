package com.examly.springapp.controller;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.JwtUtil;
import com.examly.springapp.model.GoogleLoginDTO;
import com.examly.springapp.model.User;
import com.examly.springapp.service.GoogleAuthService;
import com.examly.springapp.service.InstructorService;
import com.examly.springapp.service.OtpService;
import com.examly.springapp.service.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * POST /api/auth/google: exchanges a verified Google ID token for the same answer as /api/login
 * ({token, expiresIn, userId, role, username, email} plus the HttpOnly refresh cookie).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, log in, refresh the access token and log out")
public class GoogleAuthController {
    private static final Logger log = LoggerFactory.getLogger(GoogleAuthController.class);
    private static final String REFRESH_COOKIE = "refresh_token";

    private final GoogleAuthService googleAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final boolean secureCookie;
    private final long refreshDays;

    /** Day 1 mobile OTP (optional): a person who registered with the form and has not verified the phone yet cannot skip it through Google. */
    @Autowired(required = false)
    private OtpService otpService;

    /** Instructors may log in only after an administrator approved their application. */
    @Autowired(required = false)
    private InstructorService instructorService;

    public GoogleAuthController(GoogleAuthService googleAuthService, JwtUtil jwtUtil,
                                RefreshTokenService refreshTokenService,
                                @Value("${app.cookie.secure:false}") boolean secureCookie,
                                @Value("${jwt.refresh-expiration-days:7}") long refreshDays) {
        this.googleAuthService = googleAuthService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.secureCookie = secureCookie;
        this.refreshDays = refreshDays;
    }

    @Operation(summary = "Log in with Google",
            description = "Public. Body: {credential, mode}. credential = the Google ID token. mode \"login\" (default) works only for a registered e-mail (404 otherwise); mode \"register\" creates the CUSTOMER account (409 if it already exists).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in"),
            @ApiResponse(responseCode = "400", description = "Credential missing, or Google login is not configured"),
            @ApiResponse(responseCode = "401", description = "Google token is invalid, expired or the e-mail is not verified"),
            @ApiResponse(responseCode = "404", description = "mode login: no account is registered for this Google e-mail"),
            @ApiResponse(responseCode = "409", description = "mode register: this e-mail is already registered"),
            @ApiResponse(responseCode = "403", description = "Admin accounts cannot use Google login, or the mobile number is not verified yet")
    })
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> google(@Valid @RequestBody GoogleLoginDTO body) {
        log.trace("google login called");
        User user = body.isRegister()
                ? googleAuthService.registerWithGoogle(body.getCredential())   // creates and stores the account
                : googleAuthService.loginWithGoogle(body.getCredential());     // only for accounts that already exist
        if (otpService != null) {
            otpService.requireVerified(user);   // 403 "Please verify your mobile number to continue" while the OTP is still pending
        }
        if (instructorService != null) {
            instructorService.requireCanLogin(user);   // 403 while an instructor application is pending or rejected
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId(), user.getUsername()));
        out.put("expiresIn", jwtUtil.getAccessExpirationMs() / 1000);
        out.put("userId", user.getUserId());
        out.put("role", user.getRole());
        out.put("username", user.getUsername());
        out.put("email", user.getEmail());
        String refresh = refreshTokenService.issue(user);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refresh)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api")
                .maxAge(Duration.ofDays(refreshDays))
                .build();
        return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.SET_COOKIE, cookie.toString()).body(out);
    }
}