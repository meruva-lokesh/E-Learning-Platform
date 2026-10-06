package com.examly.springapp.controller;

import com.examly.springapp.config.AiFeatureLimiter;
import com.examly.springapp.dto.AiDtos.*;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.ChatService;
import com.examly.springapp.service.GeminiService;
import com.examly.springapp.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI quiz and AI chatbot for customers. Every call is limited per user (see AiFeatureLimiter) and the
 * customer is always taken from the login token, never from the request.
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI quiz and chatbot", description = "Gemini powered quiz and assistant (CUSTOMER only)")
public class AiAssistantController {
    private final QuizService quizService;
    private final ChatService chatService;
    private final AiFeatureLimiter limiter;
    private final AccessService access;
    private final GeminiService gemini;

    @Autowired
    public AiAssistantController(QuizService quizService, ChatService chatService, AiFeatureLimiter limiter,
                                 AccessService access, GeminiService gemini) {
        this.quizService = quizService;
        this.chatService = chatService;
        this.limiter = limiter;
        this.access = access;
        this.gemini = gemini;
    }

    @Operation(summary = "Is the AI switched on?")
    @GetMapping("/status")
    public ResponseEntity<AiStatus> status() {
        return ResponseEntity.ok(new AiStatus(gemini.isEnabled()));
    }

    @Operation(summary = "Create an AI quiz for a course",
            description = "Body: {courseId, numQuestions (3-10, default 5), difficulty EASY|MEDIUM|HARD}. The answers are not returned.")
    @PostMapping("/quiz/generate")
    public ResponseEntity<QuizResponse> generate(@RequestBody QuizRequest body) {
        var customer = access.currentCustomer();
        limiter.checkQuiz(access.currentEmail());
        return ResponseEntity.ok(quizService.generate(customer, body));
    }

    @Operation(summary = "Submit quiz answers and get the marks",
            description = "Body: {quizId, answers:[index or null per question]}. A quiz can be submitted once.")
    @PostMapping("/quiz/submit")
    public ResponseEntity<QuizResult> submit(@RequestBody QuizSubmitRequest body) {
        return ResponseEntity.ok(quizService.submit(access.currentCustomer(), body));
    }

    @Operation(summary = "My last 20 AI quiz results")
    @GetMapping("/quiz/history")
    public ResponseEntity<List<QuizHistoryItem>> history() {
        return ResponseEntity.ok(quizService.history(access.currentCustomer()));
    }

    @Operation(summary = "Ask the AI assistant",
            description = "Body: {message, history:[{role:'user'|'bot', text}]}. Falls back to a plain course search when the AI is unavailable.")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest body) {
        var customer = access.currentCustomer();
        limiter.checkChat(access.currentEmail());
        return ResponseEntity.ok(chatService.reply(customer, body));
    }
}
