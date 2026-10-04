package com.examly.springapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.Date;

/**
 * A customer's review of a course; the subject holds the course type that is reviewed.
 *
 * @author Sumit
 */
@Entity
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @NotBlank(message = "Subject is required")
    @Size(max = 100, message = "Subject is too long")
    private String subject;

    @NotBlank(message = "Feedback body is required")
    @Size(max = 2000, message = "Feedback is too long")
    @Column(length = 2000)
    private String body;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int rating;

    @Temporal(TemporalType.DATE)
    private Date dateCreated;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** No-arg constructor required by JPA and Jackson. */
    public Review() {}

    /** Convenience constructor for a new review (no id, date is set on save); chains to the all-fields constructor. */
    public Review(String subject, String body, int rating, Customer customer) {
        this(null, subject, body, rating, null, customer);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public Review(Long reviewId, String subject, String body, int rating, Date dateCreated, Customer customer) {
        this();
        this.reviewId = reviewId;
        this.subject = subject;
        this.body = body;
        this.rating = rating;
        this.dateCreated = dateCreated;
        this.customer = customer;
    }

    @PrePersist
    void onCreate() {
        if (dateCreated == null) dateCreated = new Date();
    }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
