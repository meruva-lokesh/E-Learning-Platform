package com.examly.springapp.service;

import com.examly.springapp.dto.AdminStatsDTO;
import com.examly.springapp.dto.CustomerStatsDTO;

/**
 * Read-only statistics for the customer and admin dashboards.
 *
 * @author Meruva Lokesh
 */
public interface DashboardService {
    /** Numbers for one customer's dashboard. Throws ResourceNotFoundException when the customer does not exist. */
    CustomerStatsDTO getCustomerStats(Long customerId);

    /** Platform-wide numbers for the admin dashboard. */
    AdminStatsDTO getAdminStats();
}
