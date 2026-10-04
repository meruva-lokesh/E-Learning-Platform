package com.examly.springapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.examly.springapp.dto.AdminStatsDTO;
import com.examly.springapp.dto.CustomerStatsDTO;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for the rounding helpers and the stats DTO constructors of the dashboards.
 *
 * @author Meruva Lokesh
 */
class DashboardServiceImplTest {

    @Test
    void nullSumsAndAveragesBecomeZero() {
        assertEquals(0.0, DashboardServiceImpl.round2(null), 1e-9);
        assertEquals(0.0, DashboardServiceImpl.round1(null), 1e-9);
    }

    @Test
    void moneyIsRoundedToCentsAndRatingsToOneDecimal() {
        assertEquals(1234.57, DashboardServiceImpl.round2(1234.5678), 1e-9);
        assertEquals(4.2, DashboardServiceImpl.round1(4.2499), 1e-9);
    }

    @Test
    void statsDtosKeepTheirValues() {
        CustomerStatsDTO customer = new CustomerStatsDTO(3, 2, 1, 99.5, 4);
        assertEquals(3, customer.getEnrolledCourses());
        assertEquals(2, customer.getOrdersPlaced());
        assertEquals(1, customer.getCartItems());
        assertEquals(99.5, customer.getTotalSpent(), 1e-9);
        assertEquals(4, customer.getReviewsGiven());

        AdminStatsDTO admin = new AdminStatsDTO(10, 5, 7, 1500.0, 12, 4.5);
        assertEquals(10, admin.getTotalCourses());
        assertEquals(5, admin.getTotalCustomers());
        assertEquals(7, admin.getTotalOrders());
        assertEquals(1500.0, admin.getTotalRevenue(), 1e-9);
        assertEquals(12, admin.getTotalReviews());
        assertEquals(4.5, admin.getAverageRating(), 1e-9);
    }
}
