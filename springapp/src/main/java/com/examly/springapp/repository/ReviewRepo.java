package com.examly.springapp.repository;

import com.examly.springapp.model.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Data access for reviews, including the rating summary computed in the
 * database.
 *
 * @author Sumit
 */
@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {
    List<Review> findByCustomer_User_UserId(Long userId);

    /** Reviews whose subject equals the given course type, ignoring case. */
    List<Review> findBySubjectIgnoreCase(String subject);

    /** Number of reviews written by a customer. */
    long countByCustomer_CustomerId(Long customerId);

    /** Average rating over all reviews, or null when there are none. */
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double averageRating();

    /**
     * Rating summary per course: one row per subject (case-insensitive) as
     * [subject, average rating, review count].
     * Returned as raw rows and mapped to CourseRatingDTO in the service.
     */
    @Query("SELECT MIN(r.subject), AVG(r.rating), COUNT(r) FROM Review r GROUP BY LOWER(r.subject)")
    List<Object[]> summarizeBySubject();

    /**
     * Removes every review of a course type, ignoring case; returns how many were
     * removed.
     */
    long deleteBySubjectIgnoreCase(String subject);
}
