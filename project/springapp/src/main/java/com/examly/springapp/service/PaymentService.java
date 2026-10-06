package com.examly.springapp.service;

import com.examly.springapp.dto.RazorpayVerifyDTO;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Cart;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.Orders;
import com.examly.springapp.model.Payment;
import com.examly.springapp.repository.OrderRepo;
import com.examly.springapp.repository.PaymentRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Pay for the cart with Razorpay (test mode or live mode, decided only by the keys you configure).
 * <p>
 * Step 1, {@link #createCheckout}: the SERVER reads the customer's cart, adds up the prices from the database
 * (the browser can never choose the price), creates an order at Razorpay and remembers it in the payments table.
 * <br>
 * Step 2, {@link #verifyAndEnroll}: after the customer pays, the server checks Razorpay's signature. Only then it saves
 * the real order through the existing OrderService, so the duplicate-purchase rule still applies, and empties the cart.
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    /** Razorpay cannot charge less than 1 rupee (100 paise). */
    private static final long MIN_PAISE = 100;

    private final RazorpayClient razorpay;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;
    private final InvoiceService invoiceService;
    private final EarningService earningService;

    public PaymentService(RazorpayClient razorpay, CartService cartService, OrderService orderService,
                          OrderRepo orderRepo, PaymentRepo paymentRepo,
                          InvoiceService invoiceService, EarningService earningService) {
        this.razorpay = razorpay;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
        this.invoiceService = invoiceService;
        this.earningService = earningService;
    }

    /** Creates the Razorpay order for what is in the customer's cart and returns what the browser needs to open Checkout. */
    public Map<String, Object> createCheckout(Customer customer) {
        log.trace("createCheckout entered");
        requireConfigured();
        Long customerId = customer.getCustomerId();
        Cart cart = cartService.getCartByCustomerId(customerId);
        List<Course> courses = cart.getCourses();
        if (courses == null || courses.isEmpty()) {
            throw new InvalidRequestException("Your cart is empty");
        }
        // same rule as the order service: a course can be bought only once
        Set<Long> bought = new HashSet<>();
        DatabaseOperationException.guard("loading orders", () -> orderRepo.findByCustomer_CustomerId(customerId))
                .forEach(o -> o.getCourses().forEach(c -> bought.add(c.getCourseId())));
        BigDecimal total = BigDecimal.ZERO;
        for (Course c : courses) {
            if (bought.contains(c.getCourseId())) {
                throw new DuplicateResourceException("You are already enrolled in " + c.getCourseType());
            }
            total = total.add(BigDecimal.valueOf(c.getCoursePrice() == null ? 0 : c.getCoursePrice()));
        }
        long paise = total.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (paise < MIN_PAISE) {
            throw new InvalidRequestException("Razorpay needs a total of at least Rs 1. A free cart does not need a payment.");
        }

        String receipt = ("rcpt_" + customerId + "_" + System.currentTimeMillis());
        String razorpayOrderId = razorpay.createOrder(paise, receipt.length() > 40 ? receipt.substring(0, 40) : receipt);

        Payment payment = new Payment();
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmountPaise(paise);
        payment.setCurrency("INR");
        payment.setStatus(Payment.CREATED);
        payment.setCustomerId(customerId);
        payment.setCourseIds(courses.stream().map(c -> String.valueOf(c.getCourseId())).collect(Collectors.joining(",")));
        payment.setCreatedAt(Instant.now());
        DatabaseOperationException.guard("saving a payment", () -> paymentRepo.save(payment));

        Map<String, Object> prefill = new LinkedHashMap<>();
        prefill.put("name", customer.getCustomerName());
        if (customer.getUser() != null) {
            prefill.put("email", customer.getUser().getEmail());
            String mobile = customer.getUser().getMobileNumber();
            if (mobile != null && mobile.matches("^[6-9][0-9]{9}$")) {
                prefill.put("contact", mobile);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("keyId", razorpay.getKeyId());
        out.put("razorpayOrderId", razorpayOrderId);
        out.put("amount", paise);
        out.put("currency", "INR");
        out.put("name", "Online E-Learning Platform");
        out.put("description", courses.size() + (courses.size() == 1 ? " course" : " courses"));
        out.put("prefill", prefill);
        log.info("Checkout prepared: customer {}, {} course(s), {} paise", customerId, courses.size(), paise);
        return out;
    }

    /** Checks the signature, then saves the real order. Calling it twice with the same payment returns the same order. */
    public Orders verifyAndEnroll(Customer customer, RazorpayVerifyDTO dto) {
        log.trace("verifyAndEnroll entered");
        requireConfigured();
        Payment payment = DatabaseOperationException.guard("loading a payment", () -> paymentRepo.findByRazorpayOrderId(dto.razorpayOrderId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getCustomerId().equals(customer.getCustomerId())) {
            log.warn("Payment {} belongs to another customer", payment.getPaymentId());
            throw new AccessDeniedException("This payment belongs to another customer");
        }
        if (Payment.PAID.equals(payment.getStatus()) && payment.getOrderId() != null) {
            // the browser repeated the call (double click, reload): answer with the order we already saved
            log.info("Payment {} was already verified; returning order {}", payment.getPaymentId(), payment.getOrderId());
            afterPaid(payment, payment.getOrderId());      // heals an invoice / earning that failed the first time
            return orderService.getOrderById(payment.getOrderId());
        }
        if (!RazorpaySignature.matches(dto.razorpayOrderId(), dto.razorpayPaymentId(), dto.razorpaySignature(), razorpay.getKeySecret())) {
            log.warn("Payment {}: signature check FAILED", payment.getPaymentId());
            throw new InvalidRequestException("Payment verification failed. You were not enrolled.");
        }

        // the signature is valid: the money is real. Record it before anything else can go wrong.
        payment.setRazorpayPaymentId(dto.razorpayPaymentId());
        payment.setStatus(Payment.PAID);
        payment.setPaidAt(Instant.now());
        DatabaseOperationException.guard("saving a payment", () -> paymentRepo.save(payment));

        Orders order = new Orders();
        order.setCustomer(customer);
        List<Course> stubs = new ArrayList<>();
        for (String id : payment.getCourseIds().split(",")) {
            Course c = new Course();
            c.setCourseId(Long.valueOf(id.trim()));
            stubs.add(c);
        }
        order.setCourses(stubs);
        Orders saved;
        try {
            saved = orderService.addOrder(order);   // recalculates the price from the database and applies the duplicate rule
        } catch (RuntimeException e) {
            payment.setStatus(Payment.PAID_ORDER_FAILED);
            paymentRepo.save(payment);
            log.error("Payment {} (Razorpay payment id {}) was paid but the enrollment failed: {}",
                    payment.getPaymentId(), dto.razorpayPaymentId(), e.getMessage());
            throw e;
        }
        payment.setOrderId(saved.getOrderId());
        DatabaseOperationException.guard("saving a payment", () -> paymentRepo.save(payment));

        try {
            cartService.removeAllCourses(cartService.getCartByCustomerId(customer.getCustomerId()).getCartId());
        } catch (RuntimeException e) {
            log.warn("Order {} saved but the cart could not be emptied: {}", saved.getOrderId(), e.getMessage());
        }
        afterPaid(payment, saved.getOrderId());
        log.info("Payment {} verified: order {} created for customer {}", payment.getPaymentId(), saved.getOrderId(), customer.getCustomerId());
        return saved;
    }

    /**
     * Invoice and instructor earnings come AFTER the money and the enrollment are safe. If either fails, the
     * customer is still enrolled; the error is logged and the next call (or opening the invoice) makes it again.
     */
    private void afterPaid(Payment payment, Long orderId) {
        try {
            invoiceService.ensureForPayment(payment);
        } catch (RuntimeException e) {
            log.error("Payment {}: the invoice could not be made: {}", payment.getPaymentId(), e.getMessage());
        }
        try {
            earningService.record(payment, orderId);
        } catch (RuntimeException e) {
            log.error("Payment {}: the instructor earnings could not be recorded: {}", payment.getPaymentId(), e.getMessage());
        }
    }

    private void requireConfigured() {
        if (!razorpay.isConfigured()) {
            throw new InvalidRequestException("Razorpay is not set up on this server");
        }
    }
}
