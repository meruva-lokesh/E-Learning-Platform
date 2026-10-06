package com.examly.springapp.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks that a payment really came from Razorpay.
 * <p>
 * After a payment Razorpay gives the browser a signature. It is HMAC-SHA256 of
 * {@code razorpay_order_id + "|" + razorpay_payment_id}, keyed with YOUR key secret (which only you and Razorpay know).
 * Nobody can forge it without the secret, so if our own calculation gives the same text, the payment is real.
 */
public final class RazorpaySignature {
    private RazorpaySignature() {}

    /** The signature Razorpay would have produced (lower-case hex). */
    public static String sign(String razorpayOrderId, String razorpayPaymentId, String keySecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((razorpayOrderId + "|" + razorpayPaymentId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Could not calculate the Razorpay signature", e);
        }
    }

    /** True when the signature sent by the browser equals the one we calculate. Compared in constant time. */
    public static boolean matches(String razorpayOrderId, String razorpayPaymentId, String signature, String keySecret) {
        if (razorpayOrderId == null || razorpayPaymentId == null || signature == null
                || keySecret == null || keySecret.isEmpty()) {
            return false;
        }
        byte[] expected = sign(razorpayOrderId, razorpayPaymentId, keySecret).getBytes(StandardCharsets.UTF_8);
        byte[] given = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }
}
