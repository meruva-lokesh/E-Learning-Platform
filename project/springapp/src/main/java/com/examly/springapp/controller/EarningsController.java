package com.examly.springapp.controller;

import com.examly.springapp.dto.PayDtos.*;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.EarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instructor earnings (INSTRUCTOR only) and payouts (ADMIN only). The role rules live in SecurityConfig:
 * /api/instructor/** is INSTRUCTOR and /api/admin/** is ADMIN, so no new security rule is needed for these.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Earnings", description = "Instructor earnings and admin payouts")
public class EarningsController {
    private final EarningService service;
    private final AccessService access;

    public EarningsController(EarningService service, AccessService access) {
        this.service = service;
        this.access = access;
    }

    @Operation(summary = "My earnings: totals, every sale, and the payouts I received")
    @GetMapping("/instructor/earnings")
    public ResponseEntity<EarningsSummary> mine() {
        return ResponseEntity.ok(service.forInstructor(access.currentUser()));
    }

    @Operation(summary = "Instructors who are owed money")
    @GetMapping("/admin/payouts/payable")
    public ResponseEntity<List<PayableRow>> payable() {
        return ResponseEntity.ok(service.payable());
    }

    @Operation(summary = "All payouts made so far")
    @GetMapping("/admin/payouts")
    public ResponseEntity<List<PayoutView>> history() {
        return ResponseEntity.ok(service.allPayouts());
    }

    @Operation(summary = "Record that an instructor was paid everything pending")
    @PostMapping("/admin/payouts")
    public ResponseEntity<PayoutView> pay(@RequestBody PayoutRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.pay(access.currentEmail(), body));
    }
}
