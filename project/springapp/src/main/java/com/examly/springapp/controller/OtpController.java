package com.examly.springapp.controller;

import com.examly.springapp.dto.OtpDtos.*;
import com.examly.springapp.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Mobile number verification by OTP. Public (the user cannot log in yet); abuse is limited inside OtpService. */
@RestController
@RequestMapping("/api/otp")
@Tag(name = "Mobile OTP", description = "Send and check the verification code")
public class OtpController {
    private final OtpService otp;

    @Autowired
    public OtpController(OtpService otp) {
        this.otp = otp;
    }

    @Operation(summary = "Send a verification code",
            description = "Body: {email}. The code goes to the mobile number saved at registration. The answer is the same for every email.")
    @PostMapping("/send")
    public ResponseEntity<OtpSendResponse> send(@RequestBody OtpSendRequest body, HttpServletRequest request) {
        return ResponseEntity.ok(otp.send(body == null ? null : body.email(), request.getRemoteAddr()));
    }

    @Operation(summary = "Check the verification code", description = "Body: {email, code}. 6 digits, 5 tries, valid 5 minutes.")
    @PostMapping("/verify")
    public ResponseEntity<OtpVerifyResponse> verify(@RequestBody OtpVerifyRequest body, HttpServletRequest request) {
        return ResponseEntity.ok(otp.verify(body == null ? null : body.email(), body == null ? null : body.code(), request.getRemoteAddr()));
    }

    @Operation(summary = "Is OTP switched on?")
    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Boolean>> status() {
        return ResponseEntity.ok(java.util.Map.of("required", otp.isRequired()));
    }
}
