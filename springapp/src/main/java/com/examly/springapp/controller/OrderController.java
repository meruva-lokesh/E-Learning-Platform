package com.examly.springapp.controller;

import com.examly.springapp.model.Orders;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Order endpoints (the SRS calls this controller OrdersController; the class and URL base /api/order are kept
 * as they are because the Angular app depends on them). Customers only see their own orders.
 *
 * @author Tanvi
 */
@RestController
@RequestMapping("/api/order")
@Validated
@Tag(name = "Orders", description = "Place, view and delete orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final AccessService access;

    @Autowired
    public OrderController(OrderService orderService, AccessService access) {
        this.orderService = orderService;
        this.access = access;
    }

    @Operation(summary = "Place an order", description = "CUSTOMER only. The price is calculated on the server from the course prices.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "No courses or a course has no courseId"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer"),
            @ApiResponse(responseCode = "404", description = "Customer profile or course not found")
    })
    @PostMapping
    public ResponseEntity<Orders> addOrder(@RequestBody Orders order) {
        log.trace("addOrder called");
        order.setCustomer(access.currentCustomer());
        Orders saved = orderService.addOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "List all orders", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of orders (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping
    public ResponseEntity<List<Orders>> getAllOrders() {
        log.trace("getAllOrders called");
        List<Orders> orders = orderService.getAllOrders();
        log.debug("getAllOrders: returning {} order(s)", orders.size());
        return ResponseEntity.status(HttpStatus.OK).body(orders);
    }

    @Operation(summary = "Get an order by id", description = "ADMIN, or the customer who owns the order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your order"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderById(@PathVariable @Min(value = 1, message = "must be a positive number") Long orderId) {
        log.trace("getOrderById called for orderId={}", orderId);
        Orders order = orderService.getOrderById(orderId);
        access.requireCustomerAccess(order.getCustomer().getCustomerId());
        return ResponseEntity.status(HttpStatus.OK).body(order);
    }

    @Operation(summary = "List the orders of a customer", description = "CUSTOMER only, own orders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of orders"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "No orders found for this customer")
    })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Orders>> getOrdersByCustomerId(@PathVariable @Min(value = 1, message = "must be a positive number") Long customerId) {
        log.trace("getOrdersByCustomerId called for customerId={}", customerId);
        access.requireCustomerAccess(customerId);
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrdersByCustomerId(customerId));
    }

    @Operation(summary = "Delete an order", description = "CUSTOMER only, own order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order deleted, the deleted order is returned"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your order"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Orders> deleteOrder(@PathVariable @Min(value = 1, message = "must be a positive number") Long orderId) {
        log.trace("deleteOrder called for orderId={}", orderId);
        Orders order = orderService.getOrderById(orderId);
        access.requireCustomerAccess(order.getCustomer().getCustomerId());
        return ResponseEntity.status(HttpStatus.OK).body(orderService.deleteOrder(orderId));
    }
}
