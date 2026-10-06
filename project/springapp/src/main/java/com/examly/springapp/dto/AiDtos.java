package com.examly.springapp.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request and response shapes of the AI quiz and the AI chatbot (all in one file so they are easy to find).
 * The quiz questions sent to the browser never contain the answers; those stay on the server.
 */
public final class AiDtos {
    private AiDtos() {}

    // ----- quiz -----
    public record QuizRequest(Long courseId, Integer numQuestions, String difficulty) {}

    public record QuizQuestionView(int index, String question, List<String> options) {}

    public record QuizResponse(String quizId, Long courseId, String courseTitle, String difficulty,
                               List<QuizQuestionView> questions) {}

    public record QuizSubmitRequest(String quizId, List<Integer> answers) {}

    public record QuizResultItem(int index, String question, List<String> options, Integer yourAnswer,
                                 int correctAnswer, boolean correct, String explanation) {}

    public record QuizResult(int score, int total, int percent, List<QuizResultItem> items) {}

    public record QuizHistoryItem(Long attemptId, Long courseId, String courseTitle, String difficulty,
                                  int score, int total, LocalDateTime takenAt) {}

    // ----- chatbot -----
    public record ChatTurn(String role, String text) {}

    public record ChatRequest(String message, List<ChatTurn> history) {}

    /** A short course card shown under a chatbot answer. */
    public record ChatCourse(Long courseId, String courseType, Double coursePrice) {}

    public record ChatResponse(String reply, List<ChatCourse> courses, boolean aiUsed) {}

    // ----- status -----
    public record AiStatus(boolean aiEnabled) {}
}
