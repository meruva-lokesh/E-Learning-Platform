package com.examly.springapp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limits requests per client IP: a global limit and a stricter one for login,
 * register and refresh.
 *
 * @author Suriya
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000L;
    private final RateLimiter limiter;
    private final int globalPerMinute;
    private final int authPerMinute;

    public RateLimitFilter(RateLimiter limiter, int globalPerMinute, int authPerMinute) {
        this.limiter = limiter;
        this.globalPerMinute = globalPerMinute;
        this.authPerMinute = authPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        if (path.startsWith("/api/video/stream/")) { // Day 3: protected by the signed ticket instead
            chain.doFilter(request, response);
            return;
        }
        boolean authCall = path.equals("/api/login") || path.equals("/api/register") ||
                path.equals("/api/refresh")
                || path.equals("/api/auth/google")
                || path.equals("/api/forgot-password") || path.equals("/api/reset-password")
                || path.equals("/api/instructor/status") || path.equals("/api/instructor/resubmit");
        if (authCall && !limiter.tryAcquire("auth:" + ip, authPerMinute, WINDOW_MS)) {
            reject(response, limiter.retryAfterSeconds("auth:" + ip, WINDOW_MS));
            return;
        }
        if (!limiter.tryAcquire("global:" + ip, globalPerMinute, WINDOW_MS)) {
            reject(response, limiter.retryAfterSeconds("global:" + ip, WINDOW_MS));
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please slow down.\"}");
    }
}