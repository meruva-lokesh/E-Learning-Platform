package com.examly.springapp.video;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A browser's video player cannot send the Authorization header, so the page first asks for a short-lived
 * "ticket" (with the normal login) and puts it in the video address. The ticket is signed with the server
 * secret, names exactly one video and expires, so a copied address stops working after a while.
 */
@Service
public class VideoTicketService {
    private final byte[] key;
    private final long ttlMs;

    public VideoTicketService(@Value("${jwt.secret}") String secret,
                              @Value("${video.ticket-minutes:120}") long ticketMinutes) {
        if (secret == null || secret.length() < 32) throw new IllegalStateException("jwt.secret must be at least 32 characters long");
        this.key = ("video-ticket:" + secret).getBytes(StandardCharsets.UTF_8);
        this.ttlMs = Math.max(1, ticketMinutes) * 60_000L;
    }

    public long ttlSeconds() { return ttlMs / 1000; }

    public String issue(Long videoId, Long userId) { return issue(videoId, userId, System.currentTimeMillis()); }

    public String issue(Long videoId, Long userId, long nowMs) {
        String payload = videoId + ":" + userId + ":" + (nowMs + ttlMs);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    public boolean isValid(String ticket, Long videoId) { return isValid(ticket, videoId, System.currentTimeMillis()); }

    public boolean isValid(String ticket, Long videoId, long nowMs) {
        try {
            if (ticket == null || videoId == null) return false;
            int dot = ticket.indexOf('.');
            if (dot <= 0 || dot == ticket.length() - 1) return false;
            String body = ticket.substring(0, dot);
            String sig = ticket.substring(dot + 1);
            if (!MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) return false;
            String[] parts = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8).split(":");
            if (parts.length != 3) return false;
            return Long.parseLong(parts[0]) == videoId && Long.parseLong(parts[2]) > nowMs;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign the video ticket", e);
        }
    }
}
