package com.examly.springapp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One row of GET /api/review/summary: how a course (review subject) is rated on average.
 *
 * @author Sumit
 */
public class CourseRatingDTO {
    private String courseType;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private double averageRating;

    @PositiveOrZero
    private long reviewCount;

    public CourseRatingDTO() {}

    /** All-fields constructor; chains to the no-arg constructor and rounds the average to 1 decimal. */
    public CourseRatingDTO(String courseType, double averageRating, long reviewCount) {
        this();
        this.courseType = courseType;
        this.averageRating = round1(averageRating);
        this.reviewCount = reviewCount;
    }

    /** Rounds to one decimal place, e.g. 4.26 -> 4.3. */
    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
}
