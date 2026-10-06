package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of POST /api/payment/razorpay/verify: the three values Razorpay Checkout gives the browser
 * after a successful payment.
 */
public record RazorpayVerifyDTO(
        @NotBlank(message = "razorpayOrderId is required") @Size(max = 64, message = "razorpayOrderId is too long") String razorpayOrderId,
        @NotBlank(message = "razorpayPaymentId is required") @Size(max = 64, message = "razorpayPaymentId is too long") String razorpayPaymentId,
        @NotBlank(message = "razorpaySignature is required") @Size(max = 128, message = "razorpaySignature is too long") String razorpaySignature) {
}
