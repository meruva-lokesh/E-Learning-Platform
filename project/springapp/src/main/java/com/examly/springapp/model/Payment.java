package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One Razorpay payment attempt. The server writes this row when it creates the Razorpay order
 * (so the amount and the courses are fixed on the server) and updates it after the payment is verified.
 * It is a NEW table ("payments"); no existing table or entity is changed.
 */
@Entity
@Table(name = "payments")
public class Payment {
    public static final String CREATED = "CREATED";
    public static final String PAID = "PAID";
    /** The money arrived but the enrollment could not be saved: needs a human to look at it. */
    public static final String PAID_ORDER_FAILED = "PAID_ORDER_FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, unique = true, length = 64)
    private String razorpayOrderId;

    @Column(length = 64)
    private String razorpayPaymentId;

    /** Amount in paise (100 paise = 1 rupee), as sent to Razorpay. */
    private long amountPaise;

    @Column(length = 3)
    private String currency;

    @Column(length = 30)
    private String status;

    private Long customerId;

    /** Comma separated course ids that were in the cart when the payment started, e.g. "3,5,8". */
    @Column(length = 1000)
    private String courseIds;

    /** The saved enrollment (orders.order_id) once the payment is verified. */
    private Long orderId;

    private Instant createdAt;
    private Instant paidAt;

    /** No-arg constructor required by JPA. */
    public Payment() {}

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long amountPaise) { this.amountPaise = amountPaise; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCourseIds() { return courseIds; }
    public void setCourseIds(String courseIds) { this.courseIds = courseIds; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
