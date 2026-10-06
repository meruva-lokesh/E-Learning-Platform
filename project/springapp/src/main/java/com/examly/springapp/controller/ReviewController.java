package com.examly.springapp.controller;

import com.examly.springapp.dto.CourseRatingDTO;
import com.examly.springapp.model.Review;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Review endpoints.
 *
 * <p><b>Where 204 No Content is used.</b> Only the NEW endpoint GET /api/review/course/{courseType} answers
 * 204 when a course has no reviews yet, because "nothing to show" is a normal situation there.
 * The endpoints that come from the SRS API table keep their documented behaviour (200 with data, 404 when the
 * record or list is missing, e.g. GET /api/review/user/{userId}), so the Angular app and the SRS stay valid.
 *
 * @author Sumit
 */
@RestController
@RequestMapping("/api/review")
@Validated
@Tag(name = "Reviews", description = "Course reviews and rating summary")
public class ReviewController {
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final AccessService access;

    @Autowired
    public ReviewController(ReviewService reviewService, AccessService access) {
        this.reviewService = reviewService;
        this.access = access;
    }

    @Operation(summary = "Add a review", description = "CUSTOMER only. The subject must be the type of a course the customer has bought.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review created"),
            @ApiResponse(responseCode = "400", description = "Validation failed or course not bought"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer"),
            @ApiResponse(responseCode = "404", description = "Customer profile not found")
    })
    @PostMapping
    public ResponseEntity<Review> addReview(@Valid @RequestBody Review review) {
        log.trace("addReview called");
        review.setCustomer(access.currentCustomer());
        Review saved = reviewService.addReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "List all reviews", description = "ADMIN and CUSTOMER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reviews (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        log.trace("getAllReviews called");
        List<Review> reviews = reviewService.getAllReviews();
        log.debug("getAllReviews: returning {} review(s)", reviews.size());
        return ResponseEntity.status(HttpStatus.OK).body(reviews);
    }

    /** Declared before "/{reviewId}"; the literal path "summary" always wins over the variable path. */
    @Operation(summary = "Rating summary per course",
            description = "ADMIN and CUSTOMER. One row per course: {courseType, averageRating (1 decimal), reviewCount}.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary list (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/summary")
    public ResponseEntity<List<CourseRatingDTO>> getRatingSummary() {
        log.trace("getRatingSummary called");
        List<CourseRatingDTO> summary = reviewService.getRatingSummary();
        log.debug("getRatingSummary: returning {} row(s)", summary.size());
        return ResponseEntity.status(HttpStatus.OK).body(summary);
    }

    @Operation(summary = "Reviews of one course",
            description = "ADMIN and CUSTOMER. Reviews whose subject equals the course type (ignoring case). 204 when there are none.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reviews"),
            @ApiResponse(responseCode = "204", description = "The course has no reviews"),
            @ApiResponse(responseCode = "400", description = "Course type is blank"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/course/{courseType}")
    public ResponseEntity<List<Review>> getReviewsByCourseType(
            @PathVariable @NotBlank(message = "must not be blank") String courseType) {
        log.trace("getReviewsByCourseType called for '{}'", courseType);
        List<Review> reviews = reviewService.getReviewsByCourseType(courseType);
        if (reviews.isEmpty()) {
            log.debug("getReviewsByCourseType: no reviews for '{}', answering 204", courseType);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(reviews);
    }

    @Operation(summary = "Get a review by id", description = "ADMIN and CUSTOMER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @GetMapping("/{reviewId}")
    public ResponseEntity<Review> getReviewById(@PathVariable @Min(value = 1, message = "must be a positive number") Long reviewId) {
        log.trace("getReviewById called for reviewId={}", reviewId);
        return ResponseEntity.status(HttpStatus.OK).body(reviewService.getReviewById(reviewId));
    }

    @Operation(summary = "Reviews written by a user", description = "CUSTOMER only, own data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reviews"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "No reviews found for this user")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getReviewsByUserId(@PathVariable @Min(value = 1, message = "must be a positive number") Long userId) {
        log.trace("getReviewsByUserId called for userId={}", userId);
        access.requireUserAccess(userId);
        return ResponseEntity.status(HttpStatus.OK).body(reviewService.getReviewsByUserId(userId));
    }

    @Operation(summary = "Delete a review", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review deleted, the deleted review is returned"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Review> deleteReview(@PathVariable @Min(value = 1, message = "must be a positive number") Long reviewId) {
        log.trace("deleteReview called for reviewId={}", reviewId);
        return ResponseEntity.status(HttpStatus.OK).body(reviewService.deleteReview(reviewId));
    }

    @Operation(summary = "Delete my own review", description = "CUSTOMER only. A customer can remove only a review they wrote.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review deleted, the deleted review is returned"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer, or the review belongs to someone else"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/my/{reviewId}")
    public ResponseEntity<Review> deleteMyReview(@PathVariable @Min(value = 1, message = "must be a positive number") Long reviewId) {
        log.trace("deleteMyReview called for reviewId={}", reviewId);
        Review review = reviewService.getReviewById(reviewId);
        Long ownerId = review.getCustomer() == null ? null : review.getCustomer().getCustomerId();
        if (ownerId == null || !ownerId.equals(access.currentCustomer().getCustomerId())) {
            throw new AccessDeniedException("You can only delete your own review");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reviewService.deleteReview(reviewId));
    }
}
