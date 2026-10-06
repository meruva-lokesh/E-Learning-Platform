package com.examly.springapp.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNumber;
    @Column(nullable = false, unique = true)
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    @Column(length = 120)
    private String customerName;
    @Column(length = 120)
    private String customerEmail;
    @Column(length = 80)
    private String razorpayPaymentId;

    @Column(name = "line_items", length = 4000)
    private String lines;

    private long taxablePaise;
    private long taxPaise;
    private long totalPaise;
    private int gstPercent;
    private Instant issuedAt;

    public Invoice() {}

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String v) { invoiceNumber = v; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long v) { paymentId = v; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { orderId = v; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long v) { customerId = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { customerName = v; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String v) { customerEmail = v; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String v) { razorpayPaymentId = v; }
    public String getLines() { return lines; }
    public void setLines(String v) { lines = v; }
    public long getTaxablePaise() { return taxablePaise; }
    public void setTaxablePaise(long v) { taxablePaise = v; }
    public long getTaxPaise() { return taxPaise; }
    public void setTaxPaise(long v) { taxPaise = v; }
    public long getTotalPaise() { return totalPaise; }
    public void setTotalPaise(long v) { totalPaise = v; }
    public int getGstPercent() { return gstPercent; }
    public void setGstPercent(int v) { gstPercent = v; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant v) { issuedAt = v; }
}