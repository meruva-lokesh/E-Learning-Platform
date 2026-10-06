package com.examly.springapp.controller;

import com.examly.springapp.dto.ResetDtos.*;
import com.examly.springapp.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Forgot password. Both endpoints are public (the person cannot log in); abuse is limited in the service and the rate limit filter. */
@RestController
@RequestMapping("/api")
@Tag(name = "Forgot password", description = "Direct password reset: e-mail, then new password (no e-mail is sent)")
public class PasswordResetController {
    private final PasswordResetService service;

    @Autowired
    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @Operation(summary = "Step 1: check the e-mail", description = "Body: {email}. Answer: {requireMobile}. Does not say whether the account exists.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotResponse> forgot(@RequestBody ForgotRequest body, HttpServletRequest request) {
        return ResponseEntity.ok(service.start(body == null ? null : body.email(), request.getRemoteAddr()));
    }

    @Operation(summary = "Step 2: set a new password", description = "Body: {email, newPassword, mobile?}. mobile is needed only when reset.require-mobile=true.")
    @PostMapping("/reset-password")
    public ResponseEntity<ResetResponse> reset(@RequestBody ResetRequest body, HttpServletRequest request) {
        return ResponseEntity.ok(service.reset(body == null ? null : body.email(), body == null ? null : body.newPassword(), body == null ? null : body.mobile(), request.getRemoteAddr()));
    }
}
