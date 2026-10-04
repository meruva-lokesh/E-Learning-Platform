package com.examly.springapp.controller;

import com.examly.springapp.dto.AdminStatsDTO;
import com.examly.springapp.dto.CustomerStatsDTO;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard numbers: a customer sees only their own, an admin sees the whole platform.
 *
 * @author Meruva Lokesh
 */
@RestController
@RequestMapping("/api/dashboard")
@Validated
@Tag(name = "Dashboard", description = "Statistics for the customer and admin dashboards")
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final AccessService access;

    @Autowired
    public DashboardController(DashboardService dashboardService, AccessService access) {
        this.dashboardService = dashboardService;
        this.access = access;
    }

    @Operation(summary = "Customer dashboard statistics",
            description = "CUSTOMER only, own data. Returns {enrolledCourses, ordersPlaced, cartItems, totalSpent, reviewsGiven}.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CustomerStatsDTO> getCustomerStats(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long customerId) {
        log.trace("getCustomerStats called for customerId={}", customerId);
        access.requireCustomerAccess(customerId);   // ownership: only your own numbers
        return ResponseEntity.status(HttpStatus.OK).body(dashboardService.getCustomerStats(customerId));
    }

    @Operation(summary = "Admin dashboard statistics",
            description = "ADMIN only. Returns {totalCourses, totalCustomers, totalOrders, totalRevenue, totalReviews, averageRating}.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping("/admin")
    public ResponseEntity<AdminStatsDTO> getAdminStats() {
        log.trace("getAdminStats called");
        return ResponseEntity.status(HttpStatus.OK).body(dashboardService.getAdminStats());
    }
}
