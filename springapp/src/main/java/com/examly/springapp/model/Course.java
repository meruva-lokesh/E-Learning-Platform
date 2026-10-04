package com.examly.springapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * A course that customers can browse, search, add to the cart and buy.
 *
 * @author Sivamuthu
 */
@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @NotBlank(message = "Course type is required")
    @Size(max = 100, message = "Course type is too long")
    private String courseType;

    @Pattern(regexp = "^$|^https?://.+", message = "Image URL must start with http:// or https://")
    @Size(max = 2000, message = "Image URL is too long")
    @Column(length = 2000)
    private String courseImageUrl;

    @NotBlank(message = "Course details are required")
    @Size(max = 2000, message = "Course details are too long")
    @Column(length = 2000)
    private String courseDetails;

    @NotNull(message = "Course price is required")
    @PositiveOrZero(message = "Course price cannot be negative")
    private Double coursePrice;

    /** No-arg constructor required by JPA and Jackson. */
    public Course() {}

    /** Convenience constructor for a new course (no id yet); chains to the all-fields constructor. */
    public Course(String courseType, String courseImageUrl, String courseDetails, Double coursePrice) {
        this(null, courseType, courseImageUrl, courseDetails, coursePrice);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public Course(Long courseId, String courseType, String courseImageUrl, String courseDetails, Double coursePrice) {
        this();
        this.courseId = courseId;
        this.courseType = courseType;
        this.courseImageUrl = courseImageUrl;
        this.courseDetails = courseDetails;
        this.coursePrice = coursePrice;
    }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }
    public String getCourseImageUrl() { return courseImageUrl; }
    public void setCourseImageUrl(String courseImageUrl) { this.courseImageUrl = courseImageUrl; }
    public String getCourseDetails() { return courseDetails; }
    public void setCourseDetails(String courseDetails) { this.courseDetails = courseDetails; }
    public Double getCoursePrice() { return coursePrice; }
    public void setCoursePrice(Double coursePrice) { this.coursePrice = coursePrice; }
}
