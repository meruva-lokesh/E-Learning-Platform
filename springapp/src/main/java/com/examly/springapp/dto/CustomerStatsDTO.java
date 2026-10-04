package com.examly.springapp.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Response of GET /api/dashboard/customer/{customerId}: numbers shown on a customer's dashboard.
 *
 * @author Meruva Lokesh
 */
public class CustomerStatsDTO {
    /** Distinct courses across all of the customer's orders (buying a course twice counts once). */
    @PositiveOrZero
    private long enrolledCourses;
    @PositiveOrZero
    private long ordersPlaced;
    @PositiveOrZero
    private long cartItems;
    @PositiveOrZero
    private double totalSpent;
    @PositiveOrZero
    private long reviewsGiven;

    public CustomerStatsDTO() {}

    /** All-fields constructor; chains to the no-arg constructor. */
    public CustomerStatsDTO(long enrolledCourses, long ordersPlaced, long cartItems, double totalSpent, long reviewsGiven) {
        this();
        this.enrolledCourses = enrolledCourses;
        this.ordersPlaced = ordersPlaced;
        this.cartItems = cartItems;
        this.totalSpent = totalSpent;
        this.reviewsGiven = reviewsGiven;
    }

    public long getEnrolledCourses() { return enrolledCourses; }
    public void setEnrolledCourses(long enrolledCourses) { this.enrolledCourses = enrolledCourses; }
    public long getOrdersPlaced() { return ordersPlaced; }
    public void setOrdersPlaced(long ordersPlaced) { this.ordersPlaced = ordersPlaced; }
    public long getCartItems() { return cartItems; }
    public void setCartItems(long cartItems) { this.cartItems = cartItems; }
    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
    public long getReviewsGiven() { return reviewsGiven; }
    public void setReviewsGiven(long reviewsGiven) { this.reviewsGiven = reviewsGiven; }
}
