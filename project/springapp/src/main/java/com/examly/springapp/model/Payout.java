package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A record that the admin paid an instructor everything that was pending at that moment.
 * The real money moves outside the app (bank transfer, UPI, RazorpayX); this row is the proof and the reference.
 * New table "payouts".
 */
@Entity
@Table(name = "payouts")
public class Payout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long instructorUserId;
    private long amountPaise;
    private int earningsCount;
    /** The bank / UPI transaction reference the admin typed in. */
    @Column(length = 100)
    private String reference;
    @Column(length = 120)
    private String paidByEmail;
    private Instant paidAt;

    public Payout() {}

    public Long getId() { return id; }
    public Long getInstructorUserId() { return instructorUserId; }
    public void setInstructorUserId(Long v) { instructorUserId = v; }
    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long v) { amountPaise = v; }
    public int getEarningsCount() { return earningsCount; }
    public void setEarningsCount(int v) { earningsCount = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { reference = v; }
    public String getPaidByEmail() { return paidByEmail; }
    public void setPaidByEmail(String v) { paidByEmail = v; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant v) { paidAt = v; }
}
