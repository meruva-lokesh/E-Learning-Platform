package com.examly.springapp.service;

import com.examly.springapp.dto.CourseRatingDTO;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Review;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.OrderRepo;
import com.examly.springapp.repository.ReviewRepo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Review rules: only customers who bought a course may review it; plus per-course lookup and rating summary.
 * Repository failures are wrapped into DatabaseOperationException.
 *
 * @author Sumit
 */
@Service
public class ReviewServiceImpl implements ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepo reviewRepo;
    private final CustomerRepo customerRepo;
    private final OrderRepo orderRepo;

    public ReviewServiceImpl(ReviewRepo reviewRepo, CustomerRepo customerRepo, OrderRepo orderRepo) {
        this.reviewRepo = reviewRepo;
        this.customerRepo = customerRepo;
        this.orderRepo = orderRepo;
    }

    @Override
    public Review addReview(Review review) {
        log.trace("addReview entered");
        if (review.getCustomer() == null || review.getCustomer().getCustomerId() == null) {
            throw new InvalidRequestException("A customer id is required");
        }
        Long customerId = review.getCustomer().getCustomerId();
        Customer customer = DatabaseOperationException.guard("loading a customer", () -> customerRepo.findById(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
        // Business rule: the review subject must match a course type the customer has bought.
        boolean enrolled = DatabaseOperationException.guard("loading orders", () -> orderRepo.findByCustomer_CustomerId(customerId))
                .stream()
                .flatMap(o -> o.getCourses().stream())
                .anyMatch(c -> c.getCourseType() != null && c.getCourseType().equalsIgnoreCase(review.getSubject()));
        if (!enrolled) {
            throw new InvalidRequestException("You can only review courses you are enrolled in");
        }
        review.setReviewId(null);
        review.setCustomer(customer);
        review.setDateCreated(new Date());
        Review saved = DatabaseOperationException.guard("saving a review", () -> reviewRepo.save(review));
        log.info("Review {} created by customer {} for '{}'", saved.getReviewId(), customerId, saved.getSubject());
        return saved;
    }

    @Override
    public List<Review> getAllReviews() {
        log.trace("getAllReviews entered");
        List<Review> reviews = DatabaseOperationException.guard("loading reviews", () -> reviewRepo.findAll());
        log.debug("getAllReviews returned {} review(s)", reviews.size());
        return reviews;
    }

    @Override
    public Review getReviewById(Long reviewId) {
        log.trace("getReviewById entered for reviewId={}", reviewId);
        return DatabaseOperationException.guard("loading a review", () -> reviewRepo.findById(reviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + reviewId));
    }

    @Override
    public List<Review> getReviewsByUserId(Long userId) {
        log.trace("getReviewsByUserId entered for userId={}", userId);
        List<Review> reviews = DatabaseOperationException.guard("loading reviews", () -> reviewRepo.findByCustomer_User_UserId(userId));
        log.debug("getReviewsByUserId: {} review(s) for user {}", reviews.size(), userId);
        if (reviews.isEmpty()) {
            throw new ResourceNotFoundException("No reviews found for user id " + userId);
        }
        return reviews;
    }

    @Override
    public Review deleteReview(Long reviewId) {
        log.trace("deleteReview entered for reviewId={}", reviewId);
        Review review = getReviewById(reviewId);
        DatabaseOperationException.guardVoid("deleting a review", () -> reviewRepo.delete(review));
        log.info("Review {} deleted", reviewId);
        return review;
    }

    /** Returns an empty list (not an error) when nobody reviewed the course; the controller turns that into 204. */
    @Override
    public List<Review> getReviewsByCourseType(String courseType) {
        log.trace("getReviewsByCourseType entered for '{}'", courseType);
        if (courseType == null || courseType.isBlank()) {
            throw new InvalidRequestException("Course type is required");
        }
        String subject = courseType.trim();
        List<Review> reviews = DatabaseOperationException.guard("loading reviews", () -> reviewRepo.findBySubjectIgnoreCase(subject));
        log.debug("getReviewsByCourseType: {} review(s) for '{}'", reviews.size(), subject);
        return reviews;
    }

    @Override
    public List<CourseRatingDTO> getRatingSummary() {
        log.trace("getRatingSummary entered");
        List<Object[]> rows = DatabaseOperationException.guard("summarising ratings", () -> reviewRepo.summarizeBySubject());
        List<CourseRatingDTO> summary = new ArrayList<>();
        for (Object[] row : rows) {
            // row = [subject, average rating, review count]
            String subject = (String) row[0];
            double average = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            summary.add(new CourseRatingDTO(subject, average, count));
        }
        summary.sort(Comparator.comparing(CourseRatingDTO::getCourseType, String.CASE_INSENSITIVE_ORDER));
        log.debug("getRatingSummary returned {} course(s)", summary.size());
        return summary;
    }
}
