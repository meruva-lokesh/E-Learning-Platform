package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One line of the instructor earnings ledger: "this paid order contained this instructor's course, so the
 * instructor is owed this much". New table "course_earnings"; nothing existing is changed.
 * Courses made by an admin have no owner, so they never get a row here (the platform keeps all of it).
 * Money is stored in paise (100 paise = 1 rupee) so there are no rounding surprises.
 */
@Entity
@Table(name = "course_earnings", uniqueConstraints = @UniqueConstraint(name = "uk_earning_payment_course", columnNames = {"paymentId", "courseId"}))
public class CourseEarning {
    public static final String PENDING = "PENDING";
    public static final String PAID_OUT = "PAID_OUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;
    private Long orderId;
    @Column(nullable = false)
    private Long courseId;
    @Column(length = 200)
    private String courseTitle;
    @Column(nullable = false)
    private Long instructorUserId;

    /** What the customer paid for this course. */
    private long grossPaise;
    /** The platform's share. */
    private long feePaise;
    /** The instructor's share (gross minus fee). */
    private long ownerPaise;

    @Column(length = 20)
    private String status;
    private Long payoutId;
    private Instant createdAt;

    public CourseEarning() {}

    public Long getId() { return id; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long v) { paymentId = v; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { orderId = v; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long v) { courseId = v; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String v) { courseTitle = v; }
    public Long getInstructorUserId() { return instructorUserId; }
    public void setInstructorUserId(Long v) { instructorUserId = v; }
    public long getGrossPaise() { return grossPaise; }
    public void setGrossPaise(long v) { grossPaise = v; }
    public long getFeePaise() { return feePaise; }
    public void setFeePaise(long v) { feePaise = v; }
    public long getOwnerPaise() { return ownerPaise; }
    public void setOwnerPaise(long v) { ownerPaise = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Long getPayoutId() { return payoutId; }
    public void setPayoutId(Long v) { payoutId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
}
