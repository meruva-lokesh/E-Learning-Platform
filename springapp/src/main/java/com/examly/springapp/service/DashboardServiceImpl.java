package com.examly.springapp.service;

import com.examly.springapp.dto.AdminStatsDTO;
import com.examly.springapp.dto.CustomerStatsDTO;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Cart;
import com.examly.springapp.repository.CartRepo;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.OrderRepo;
import com.examly.springapp.repository.ReviewRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds dashboard numbers from COUNT / SUM / AVG queries, so no full tables are loaded into memory.
 * Repository failures are wrapped into DatabaseOperationException.
 *
 * @author Meruva Lokesh
 */
@Service
public class DashboardServiceImpl implements DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final CourseRepo courseRepo;
    private final CustomerRepo customerRepo;
    private final OrderRepo orderRepo;
    private final ReviewRepo reviewRepo;
    private final CartRepo cartRepo;

    public DashboardServiceImpl(CourseRepo courseRepo, CustomerRepo customerRepo, OrderRepo orderRepo,
                                ReviewRepo reviewRepo, CartRepo cartRepo) {
        this.courseRepo = courseRepo;
        this.customerRepo = customerRepo;
        this.orderRepo = orderRepo;
        this.reviewRepo = reviewRepo;
        this.cartRepo = cartRepo;
    }

    @Override
    public CustomerStatsDTO getCustomerStats(Long customerId) {
        log.trace("getCustomerStats entered for customerId={}", customerId);
        boolean exists = DatabaseOperationException.guard("checking the customer", () -> customerRepo.existsById(customerId));
        if (!exists) {
            throw new ResourceNotFoundException("Customer not found with id " + customerId);
        }
        // Business rule: "enrolled" means a course appears in at least one of the customer's orders (counted once).
        long enrolled = DatabaseOperationException.guard("counting courses", () -> orderRepo.countDistinctCoursesByCustomerId(customerId));
        long orders = DatabaseOperationException.guard("counting orders", () -> orderRepo.countByCustomer_CustomerId(customerId));
        Double spent = DatabaseOperationException.guard("summing orders", () -> orderRepo.sumOrderPriceByCustomerId(customerId));
        long reviews = DatabaseOperationException.guard("counting reviews", () -> reviewRepo.countByCustomer_CustomerId(customerId));
        Cart cart = DatabaseOperationException.guard("loading the cart", () -> cartRepo.findByCustomer_CustomerId(customerId)).orElse(null);
        long cartItems = cart == null || cart.getCourses() == null ? 0 : cart.getCourses().size();
        CustomerStatsDTO stats = new CustomerStatsDTO(enrolled, orders, cartItems, round2(spent), reviews);
        log.debug("getCustomerStats for customer {}: enrolled={}, orders={}, cartItems={}, reviews={}",
                customerId, enrolled, orders, cartItems, reviews);
        return stats;
    }

    @Override
    public AdminStatsDTO getAdminStats() {
        log.trace("getAdminStats entered");
        long courses = DatabaseOperationException.guard("counting courses", () -> courseRepo.count());
        long customers = DatabaseOperationException.guard("counting customers", () -> customerRepo.count());
        long orders = DatabaseOperationException.guard("counting orders", () -> orderRepo.count());
        Double revenue = DatabaseOperationException.guard("summing orders", () -> orderRepo.sumAllOrderPrice());
        long reviews = DatabaseOperationException.guard("counting reviews", () -> reviewRepo.count());
        Double average = DatabaseOperationException.guard("averaging ratings", () -> reviewRepo.averageRating());
        AdminStatsDTO stats = new AdminStatsDTO(courses, customers, orders, round2(revenue), reviews, round1(average));
        log.debug("getAdminStats: courses={}, customers={}, orders={}, reviews={}", courses, customers, orders, reviews);
        return stats;
    }

    /** Money rounded to cents; null (no rows) becomes 0. */
    static double round2(Double value) {
        return value == null ? 0.0 : Math.round(value * 100.0) / 100.0;
    }

    /** Ratings rounded to one decimal; null (no reviews) becomes 0. */
    static double round1(Double value) {
        return value == null ? 0.0 : Math.round(value * 10.0) / 10.0;
    }
}
