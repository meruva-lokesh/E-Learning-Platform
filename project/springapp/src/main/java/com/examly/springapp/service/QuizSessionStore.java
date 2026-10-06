package com.examly.springapp.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Keeps the answer key of every quiz that has been generated but not yet submitted. The browser only
 * gets the questions, so nobody can read the answers from the network tab. A quiz can be submitted
 * once, only by the customer it was made for, and expires after a while. Kept in memory: a server
 * restart simply makes unfinished quizzes expire (the customer generates a new one).
 */
@Component
public class QuizSessionStore {
    private static final int MAX_OPEN_QUIZZES = 5000;

    /** The private data of one open quiz. */
    public record Session(Long customerId, Long courseId, String courseTitle, String difficulty,
                          List<QuizService.Question> questions, long expiresAt) {}

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final LongSupplier clock;

    @Autowired
    public QuizSessionStore(@Value("${ai.quiz.ttl-minutes:30}") long ttlMinutes) {
        this(ttlMinutes * 60_000L, System::currentTimeMillis);
    }

    QuizSessionStore(long ttlMs, LongSupplier clock) {
        this.ttlMs = ttlMs;
        this.clock = clock;
    }

    /** Stores a new quiz and returns its random id. */
    public String put(Long customerId, Long courseId, String courseTitle, String difficulty,
                      List<QuizService.Question> questions) {
        purgeExpired();
        if (sessions.size() >= MAX_OPEN_QUIZZES) {
            sessions.clear(); // safety valve against memory growth; customers just generate again
        }
        String id = UUID.randomUUID().toString();
        sessions.put(id, new Session(customerId, courseId, courseTitle, difficulty, questions, clock.getAsLong() + ttlMs));
        return id;
    }

    /** Returns and removes the quiz (single use), or null if unknown, expired or someone else's. */
    public Session take(String quizId, Long customerId) {
        if (quizId == null) return null;
        Session s = sessions.get(quizId);
        if (s == null || !s.customerId().equals(customerId)) return null;
        if (!sessions.remove(quizId, s)) return null; // somebody else just took it
        return s.expiresAt() < clock.getAsLong() ? null : s;
    }

    public int openCount() {
        return sessions.size();
    }

    private void purgeExpired() {
        long now = clock.getAsLong();
        sessions.values().removeIf(s -> s.expiresAt() < now);
    }
}
