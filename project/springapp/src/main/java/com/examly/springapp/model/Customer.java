package com.examly.springapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Profile of a user who registered as CUSTOMER (display name and short information).
 *
 * @author Meruva Lokesh
 */
@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true)
    private Long customerId;

    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Name is too long")
    private String customerName;

    @NotBlank(message = "Information is required")
    @Size(max = 500, message = "Information is too long")
    private String information;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** No-arg constructor required by JPA and Jackson. */
    public Customer() {}

    /** Convenience constructor for a new profile (no id yet); chains to the all-fields constructor. */
    public Customer(String customerName, String information, User user) {
        this(null, customerName, information, user);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public Customer(Long customerId, String customerName, String information, User user) {
        this();
        this.customerId = customerId;
        this.customerName = customerName;
        this.information = information;
        this.user = user;
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getInformation() { return information; }
    public void setInformation(String information) { this.information = information; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
