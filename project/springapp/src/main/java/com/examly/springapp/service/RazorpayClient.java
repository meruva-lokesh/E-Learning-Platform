package com.examly.springapp.service;

import com.examly.springapp.exception.ApiCommunicationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Talks to Razorpay's server. Only one call is needed: "create an order" (POST /v1/orders).
 * It uses the JDK's own HttpClient, so no extra library is required. The key id and key secret
 * come from application.properties / environment variables and are never logged.
 */
@Component
public class RazorpayClient {
    private static final Logger log = LoggerFactory.getLogger(RazorpayClient.class);

    private final String keyId;
    private final String keySecret;
    private final String apiBase;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public RazorpayClient(@Value("${razorpay.key-id:}") String keyId,
                          @Value("${razorpay.key-secret:}") String keySecret,
                          @Value("${razorpay.api-base:https://api.razorpay.com}") String apiBase) {
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret.trim();
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        if (!isConfigured()) {
            log.warn("razorpay.key-id / razorpay.key-secret are not set: Razorpay payments are switched off");
        }
    }

    public boolean isConfigured() {
        return !keyId.isEmpty() && !keySecret.isEmpty();
    }

    /** The public key id (rzp_test_...). It is safe to send to the browser. */
    public String getKeyId() {
        return keyId;
    }

    /** The secret used to check signatures. Never send this anywhere. */
    String getKeySecret() {
        return keySecret;
    }

    /**
     * Creates an order at Razorpay and returns its id (looks like order_Abc123...).
     *
     * @param amountPaise amount in paise (Rs 499 = 49900)
     * @param receipt     our own reference, at most 40 characters
     */
    public String createOrder(long amountPaise, String receipt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", "INR");
        body.put("receipt", receipt);
        try {
            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/v1/orders"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + auth)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // the body may explain the problem (for example wrong keys); it never contains our secret
                log.warn("Razorpay refused to create an order: HTTP {} {}", response.statusCode(), response.body());
                throw new ApiCommunicationException("Razorpay returned HTTP " + response.statusCode(), null);
            }
            JsonNode json = mapper.readTree(response.body());
            String id = json.path("id").asText("");
            if (!id.startsWith("order_")) {
                throw new ApiCommunicationException("Razorpay answered without an order id", null);
            }
            log.info("Razorpay order {} created for {} paise", id, amountPaise);
            return id;
        } catch (IOException e) {
            throw new ApiCommunicationException("Could not reach Razorpay", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiCommunicationException("Razorpay call was interrupted", e);
        }
    }
}
