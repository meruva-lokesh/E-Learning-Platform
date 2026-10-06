package com.examly.springapp.controller;

import com.examly.springapp.dto.RazorpayVerifyDTO;
import com.examly.springapp.model.Orders;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Razorpay payment for the cart. CUSTOMER only. The customer is always taken from the login token,
 * never from the request body.
 */
@RestController
@RequestMapping("/api/payment/razorpay")
@Tag(name = "Payments", description = "Pay for the cart with Razorpay")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final AccessService access;

    public PaymentController(PaymentService paymentService, AccessService access) {
        this.paymentService = paymentService;
        this.access = access;
    }

    @Operation(summary = "Start a Razorpay payment for my cart",
            description = "CUSTOMER only. Prices the cart on the server and creates a Razorpay order. Returns what Razorpay Checkout needs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Razorpay order created"),
            @ApiResponse(responseCode = "400", description = "Cart is empty / total below Rs 1 / Razorpay not configured"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer"),
            @ApiResponse(responseCode = "404", description = "No cart or no customer profile"),
            @ApiResponse(responseCode = "409", description = "A course in the cart is already bought"),
            @ApiResponse(responseCode = "502", description = "Razorpay could not be reached or refused the request")
    })
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrder() {
        log.trace("razorpay createOrder called");
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.createCheckout(access.currentCustomer()));
    }

    @Operation(summary = "Verify a Razorpay payment and enroll",
            description = "CUSTOMER only. Checks Razorpay's signature; if valid, saves the order and empties the cart. Safe to call twice.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment verified, order saved (the order is returned)"),
            @ApiResponse(responseCode = "400", description = "Signature does not match, or a field is missing"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer, or the payment belongs to someone else"),
            @ApiResponse(responseCode = "404", description = "Unknown Razorpay order id"),
            @ApiResponse(responseCode = "409", description = "A course was already bought")
    })
    @PostMapping("/verify")
    public ResponseEntity<Orders> verify(@Valid @RequestBody RazorpayVerifyDTO body) {
        log.trace("razorpay verify called");
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.verifyAndEnroll(access.currentCustomer(), body));
    }
}
