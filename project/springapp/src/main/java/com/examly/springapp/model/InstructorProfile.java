package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * The application of an instructor: what they told us about themselves and where the review stands.
 * status is PENDING, APPROVED or REJECTED; a rejection always carries a reason for the instructor to read.
 * The login account itself is an ordinary row in users with role INSTRUCTOR.
 */
@Entity
@Table(name = "instructor_profiles")
public class InstructorProfile {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 100)
    private String qualification;

    private int experienceYears;

    @Column(length = 150)
    private String expertise;

    @Column(length = 1000)
    private String bio;

    @Column(length = 300)
    private String profileLink;

    @Column(length = 20, nullable = false)
    private String status = PENDING;

    @Column(length = 500)
    private String rejectionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    @Column(length = 120)
    private String reviewedBy;

    public InstructorProfile() {}

    public Long getProfileId() { return profileId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getProfileLink() { return profileLink; }
    public void setProfileLink(String profileLink) { this.profileLink = profileLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}
