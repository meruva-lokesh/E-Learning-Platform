package com.examly.springapp.sms;

import com.examly.springapp.exception.ApiCommunicationException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sends the SMS through Twilio's REST API (Programmable Messaging). No extra library is needed: it is one
 * HTTPS form post with Basic authentication (account SID and auth token). Active when otp.provider=twilio.
 * The three Twilio values come from environment variables and must never be written in a file.
 */
@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public TwilioSmsSender(@Value("${twilio.account-sid:}") String accountSid,
                           @Value("${twilio.auth-token:}") String authToken,
                           @Value("${twilio.from-number:}") String fromNumber,
                           @Value("${twilio.base-url:https://api.twilio.com}") String baseUrl) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new IllegalStateException(
                    "otp.provider=twilio needs TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER");
        }
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public void send(String toE164, String message) {
        String form = "To=" + enc(toE164) + "&From=" + enc(fromNumber) + "&Body=" + enc(message);
        String basic = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basic)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                // the body names the reason (for example an unverified trial number); it holds no secret
                log.warn("Twilio refused the SMS: status {} body {}", res.statusCode(), res.body());
                throw new ApiCommunicationException("Twilio responded with status " + res.statusCode(), null);
            }
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiCommunicationException("Twilio request was interrupted", e);
        } catch (Exception e) {
            throw new ApiCommunicationException("Twilio request failed", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
