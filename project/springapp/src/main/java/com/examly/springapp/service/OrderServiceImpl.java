package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Orders;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.OrderRepo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Order rules: an order needs a customer and at least one existing course, and its price is always
 * computed on the server. Repository failures are wrapped into DatabaseOperationException.
 *
 * @author Tanvi
 */
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepo orderRepo;
    private final CustomerRepo customerRepo;
    private final CourseRepo courseRepo;

    public OrderServiceImpl(OrderRepo orderRepo, CustomerRepo customerRepo, CourseRepo courseRepo) {
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.courseRepo = courseRepo;
    }

    @Override
    public Orders addOrder(Orders order) {
        log.trace("addOrder entered");
        if (order.getCustomer() == null || order.getCustomer().getCustomerId() == null) {
            throw new InvalidRequestException("A customer id is required");
        }
        if (order.getCourses() == null || order.getCourses().isEmpty()) {
            throw new InvalidRequestException("An order needs at least one course");
        }
        Long customerId = order.getCustomer().getCustomerId();
        Customer customer = DatabaseOperationException.guard("loading a customer", () -> customerRepo.findById(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
        // NEW: a course can be bought only once. Collect what this customer has already bought.
        Set<Long> alreadyBought = new HashSet<>();
        DatabaseOperationException.guard("loading orders", () -> orderRepo.findByCustomer_CustomerId(customerId))
                .forEach(o -> o.getCourses().forEach(c -> alreadyBought.add(c.getCourseId())));
        Set<Long> inThisOrder = new HashSet<>();
        List<Course> courses = new ArrayList<>();
        double total = 0;
        for (Course c : order.getCourses()) {
            if (c == null || c.getCourseId() == null) {
                throw new InvalidRequestException("Each course must have a courseId");
            }
            Long courseId = c.getCourseId();
            Course found = DatabaseOperationException.guard("loading a course", () -> courseRepo.findById(courseId))
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));
            // NEW: refuse a course the customer already owns, and the same course twice in one order
            if (alreadyBought.contains(courseId) || !inThisOrder.add(courseId)) {
                throw new DuplicateResourceException("You are already enrolled in " + found.getCourseType());
            }
            courses.add(found);
            total += found.getCoursePrice() == null ? 0 : found.getCoursePrice();
        }
        log.debug("addOrder: customerId={}, courses={}, computed total={}", customerId, courses.size(), total);
        order.setOrderId(null);
        order.setCustomer(customer);
        order.setCourses(courses);
        order.setOrderPrice(total);   // price is always computed on the server
        Orders saved = DatabaseOperationException.guard("saving an order", () -> orderRepo.save(order));
        log.info("Order {} created for customer {}", saved.getOrderId(), customerId);
        return saved;
    }

    @Override
    public List<Orders> getAllOrders() {
        log.trace("getAllOrders entered");
        List<Orders> orders = DatabaseOperationException.guard("loading orders", () -> orderRepo.findAll());
        log.debug("getAllOrders returned {} order(s)", orders.size());
        return orders;
    }

    @Override
    public Orders getOrderById(Long orderId) {
        log.trace("getOrderById entered for orderId={}", orderId);
        return DatabaseOperationException.guard("loading an order", () -> orderRepo.findById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
    }

    @Override
    public List<Orders> getOrdersByCustomerId(Long customerId) {
        log.trace("getOrdersByCustomerId entered for customerId={}", customerId);
        List<Orders> orders = DatabaseOperationException.guard("loading orders", () -> orderRepo.findByCustomer_CustomerId(customerId));
        log.debug("getOrdersByCustomerId: {} order(s) for customer {}", orders.size(), customerId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for customer id " + customerId);
        }
        return orders;
    }

    @Override
    public Orders deleteOrder(Long orderId) {
        log.trace("deleteOrder entered for orderId={}", orderId);
        Orders order = getOrderById(orderId);
        DatabaseOperationException.guardVoid("deleting an order", () -> orderRepo.delete(order));
        log.info("Order {} deleted", orderId);
        return order;
    }
}
