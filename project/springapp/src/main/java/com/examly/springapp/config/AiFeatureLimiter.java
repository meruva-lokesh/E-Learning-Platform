package com.examly.springapp.config;

import com.examly.springapp.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-user limits for the AI quiz and the AI chatbot (separate counters from the AI search, so a
 * chat cannot use up someone's searches). Built on the same {@link RateLimiter} as the AI search.
 */
@Component
public class AiFeatureLimiter {
    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final RateLimiter limiter;
    private final int quizPerMinute, quizPerDay, chatPerMinute, chatPerDay;

    public AiFeatureLimiter(RateLimiter limiter,
                            @Value("${ai.quiz.limit.per-minute:3}") int quizPerMinute,
                            @Value("${ai.quiz.limit.per-day:20}") int quizPerDay,
                            @Value("${ai.chat.limit.per-minute:10}") int chatPerMinute,
                            @Value("${ai.chat.limit.per-day:100}") int chatPerDay) {
        this.limiter = limiter;
        this.quizPerMinute = quizPerMinute;
        this.quizPerDay = quizPerDay;
        this.chatPerMinute = chatPerMinute;
        this.chatPerDay = chatPerDay;
    }

    public void checkQuiz(String userKey) {
        check("aiquiz", "AI quiz", userKey, quizPerMinute, quizPerDay);
    }

    public void checkChat(String userKey) {
        check("aichat", "AI chat", userKey, chatPerMinute, chatPerDay);
    }

    private void check(String prefix, String label, String userKey, int perMinute, int perDay) {
        String minuteKey = prefix + ":min:" + userKey;
        String dayKey = prefix + ":day:" + userKey;
        if (!limiter.tryAcquire(minuteKey, perMinute, MINUTE_MS)) {
            throw new RateLimitExceededException(
                    label + " limit reached: at most " + perMinute + " per minute.",
                    limiter.retryAfterSeconds(minuteKey, MINUTE_MS));
        }
        if (!limiter.tryAcquire(dayKey, perDay, DAY_MS)) {
            throw new RateLimitExceededException(
                    "Daily " + label + " limit reached (" + perDay + " per day). Try again tomorrow.",
                    limiter.retryAfterSeconds(dayKey, DAY_MS));
        }
    }
}
