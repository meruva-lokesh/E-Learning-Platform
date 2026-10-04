package com.examly.springapp.service;

import com.examly.springapp.dto.CourseRatingDTO;
import com.examly.springapp.model.Review;
import java.util.List;

/**
 * Business operations on course reviews and rating summaries.
 *
 * @author Sumit
 */
public interface ReviewService {
    Review addReview(Review review);
    List<Review> getAllReviews();
    Review getReviewById(Long reviewId);
    List<Review> getReviewsByUserId(Long userId);
    Review deleteReview(Long reviewId);

    /** Reviews whose subject equals the course type (ignoring case). Returns an empty list when there are none. */
    List<Review> getReviewsByCourseType(String courseType);

    /** Average rating and review count per course, computed in the database. */
    List<CourseRatingDTO> getRatingSummary();
}
