package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Cart;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;
import com.examly.springapp.repository.CartRepo;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cart rules: one cart per customer, no duplicate courses, total always recomputed on the server.
 * Repository failures are wrapped into DatabaseOperationException.
 *
 * @author Shoryan
 */
@Service
public class CartServiceImpl implements CartService {
    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepo cartRepo;
    private final CourseRepo courseRepo;
    private final CustomerRepo customerRepo;

    public CartServiceImpl(CartRepo cartRepo, CourseRepo courseRepo, CustomerRepo customerRepo) {
        this.cartRepo = cartRepo;
        this.courseRepo = courseRepo;
        this.customerRepo = customerRepo;
    }

    /** Looks every incoming course up in the database (so prices cannot be faked) and drops duplicates. */
    private List<Course> resolveCourses(List<Course> incoming) {
        List<Course> resolved = new ArrayList<>();
        if (incoming == null) return resolved;
        for (Course c : incoming) {
            if (c == null || c.getCourseId() == null) {
                throw new InvalidRequestException("Each course must have a courseId");
            }
            Long id = c.getCourseId();
            Course found = DatabaseOperationException.guard("loading a course", () -> courseRepo.findById(id))
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + id));
            if (resolved.stream().noneMatch(r -> r.getCourseId().equals(found.getCourseId()))) {
                resolved.add(found);
            }
        }
        return resolved;
    }

    /** Business rule: the cart total is the sum of the course prices, recalculated on every change. */
    private Cart recalc(Cart cart) {
        cart.setTotalAmount(cart.getCourses().stream()
                .mapToDouble(c -> c.getCoursePrice() == null ? 0 : c.getCoursePrice()).sum());
        return DatabaseOperationException.guard("saving the cart", () -> cartRepo.save(cart));
    }

    /** Creates the customer's cart, or merges the given courses into the cart they already have. */
    @Override
    public Cart addCart(Cart cart) {
        log.trace("addCart entered");
        if (cart.getCustomer() == null || cart.getCustomer().getCustomerId() == null) {
            throw new InvalidRequestException("A customer id is required");
        }
        Long customerId = cart.getCustomer().getCustomerId();
        Customer customer = DatabaseOperationException.guard("loading a customer", () -> customerRepo.findById(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
        Cart target = DatabaseOperationException.guard("loading the cart", () -> cartRepo.findByCustomer_CustomerId(customerId))
                .orElseGet(() -> {
                    Cart fresh = new Cart();
                    fresh.setCustomer(customer);
                    return fresh;
                });
        List<Course> toAdd = resolveCourses(cart.getCourses());
        log.debug("addCart: customerId={}, incoming courses={}", customerId, toAdd.size());
        for (Course course : toAdd) {
            // a course can only be in a cart once
            if (target.getCourses().stream().noneMatch(c -> c.getCourseId().equals(course.getCourseId()))) {
                target.getCourses().add(course);
            }
        }
        Cart saved = recalc(target);
        log.info("Cart {} saved for customer {} with {} course(s)", saved.getCartId(), customerId, saved.getCourses().size());
        return saved;
    }

    @Override
    public Cart updateCart(Long cartId, Cart cart) {
        log.trace("updateCart entered for cartId={}", cartId);
        Cart existing = getCartById(cartId);
        existing.setCourses(resolveCourses(cart.getCourses()));
        Cart saved = recalc(existing);
        log.info("Cart {} updated, now {} course(s)", cartId, saved.getCourses().size());
        return saved;
    }

    @Override
    public Cart removeCourse(Long cartId, Long courseId) {
        log.trace("removeCourse entered for cartId={}, courseId={}", cartId, courseId);
        Cart cart = getCartById(cartId);
        boolean removed = cart.getCourses().removeIf(c -> c.getCourseId().equals(courseId));
        if (!removed) throw new ResourceNotFoundException("Course " + courseId + " is not in this cart");
        Cart saved = recalc(cart);
        log.info("Course {} removed from cart {}", courseId, cartId);
        return saved;
    }

    @Override
    public Cart removeAllCourses(Long cartId) {
        log.trace("removeAllCourses entered for cartId={}", cartId);
        Cart cart = getCartById(cartId);
        cart.getCourses().clear();
        Cart saved = recalc(cart);
        log.info("Cart {} emptied", cartId);
        return saved;
    }

    @Override
    public Cart getCartById(Long cartId) {
        log.trace("getCartById entered for cartId={}", cartId);
        return DatabaseOperationException.guard("loading a cart", () -> cartRepo.findById(cartId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id " + cartId));
    }

    @Override
    public Cart getCartByUserId(Long userId) {
        log.trace("getCartByUserId entered for userId={}", userId);
        return DatabaseOperationException.guard("loading a cart", () -> cartRepo.findByCustomer_User_UserId(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id " + userId));
    }

    @Override
    public Cart getCartByCustomerId(Long customerId) {
        log.trace("getCartByCustomerId entered for customerId={}", customerId);
        return DatabaseOperationException.guard("loading a cart", () -> cartRepo.findByCustomer_CustomerId(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer id " + customerId));
    }

    @Override
    public List<Cart> getAllCarts() {
        log.trace("getAllCarts entered");
        List<Cart> carts = DatabaseOperationException.guard("loading carts", () -> cartRepo.findAll());
        log.debug("getAllCarts returned {} cart(s)", carts.size());
        return carts;
    }
}
