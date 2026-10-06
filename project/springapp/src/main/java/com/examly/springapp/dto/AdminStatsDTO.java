package com.examly.springapp.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Response of GET /api/dashboard/admin: platform-wide numbers for the admin dashboard.
 *
 * @author Meruva Lokesh
 */
public class AdminStatsDTO {
    @PositiveOrZero
    private long totalCourses;
    @PositiveOrZero
    private long totalCustomers;
    @PositiveOrZero
    private long totalOrders;
    @PositiveOrZero
    private double totalRevenue;
    @PositiveOrZero
    private long totalReviews;
    /** Mean of all review ratings, one decimal; 0 when there are no reviews. */
    @PositiveOrZero
    private double averageRating;

    public AdminStatsDTO() {}

    /** All-fields constructor; chains to the no-arg constructor. */
    public AdminStatsDTO(long totalCourses, long totalCustomers, long totalOrders, double totalRevenue,
                         long totalReviews, double averageRating) {
        this();
        this.totalCourses = totalCourses;
        this.totalCustomers = totalCustomers;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
    }

    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
}
