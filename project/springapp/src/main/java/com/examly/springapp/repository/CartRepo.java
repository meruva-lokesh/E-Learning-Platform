package com.examly.springapp.repository;

import com.examly.springapp.model.Cart;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for shopping carts.
 *
 * @author Shoryan
 */
@Repository
public interface CartRepo extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomer_CustomerId(Long customerId);
    Optional<Cart> findByCustomer_User_UserId(Long userId);
    List<Cart> findByCourses_CourseId(Long courseId);
}
