package com.examly.springapp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.examly.springapp.exception.ApiCommunicationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin client for the Gemini embedding and generation API with a daily call budget.
 *
 * @author Sumit
 */
@Service
public class GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    /** Standing instructions for the OpenAI-compatible provider; the task itself is in the user message. */
    private static final String SYSTEM_PROMPT =
            "You are the AI assistant of an online e-learning platform used by students in India. "
            + "Follow the formatting instructions in the user's message exactly. "
            + "Give the final answer directly: do not show your reasoning, do not add a preface or a closing remark. "
            + "When the user asks for JSON, reply with one valid JSON object only: no markdown fences and no text before or after it. "
            + "Be accurate. If you are not sure of a fact, say so briefly instead of guessing. Use plain, friendly English.";
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String embeddingModel;
    private final String generationModel;
    /** Optional OpenAI-compatible provider (Groq, OpenRouter, ...). When llm.api.key is set it answers all text generation. */
    private final String llmKey;
    private final String llmBase;
    private final String llmModel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int dailyBudget;
    private final AtomicInteger used = new AtomicInteger();
    private volatile LocalDate budgetDay = LocalDate.now();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Autowired
    public GeminiService(@Value("${gemini.api.key:}") String apiKey,
                         @Value("${gemini.embedding.model:text-embedding-004}") String embeddingModel,
                         @Value("${gemini.generation.model:gemini-2.5-flash}") String generationModel,
                         @Value("${ai.daily-embedding-budget:2000}") int dailyBudget,
                         @Value("${llm.api.key:}") String llmKey,
                         @Value("${llm.base-url:https://api.groq.com/openai/v1}") String llmBase,
                         @Value("${llm.model:llama-3.3-70b-versatile}") String llmModel) {
        this.dailyBudget = dailyBudget;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.embeddingModel = embeddingModel;
        this.generationModel = generationModel;
        this.llmKey = llmKey == null ? "" : llmKey.trim();
        this.llmBase = llmBase == null ? "" : (llmBase.trim().endsWith("/") ? llmBase.trim().substring(0, llmBase.trim().length() - 1) : llmBase.trim());
        this.llmModel = llmModel == null ? "" : llmModel.trim();
    }

    /** Gemini only (no other provider). */
    public GeminiService(String apiKey, String embeddingModel, String generationModel, int dailyBudget) {
        this(apiKey, embeddingModel, generationModel, dailyBudget, "", "", "");
    }

    /** True when text generation (quiz, chatbot) is available: an llm.api.key or a gemini.api.key is configured. */
    public boolean isEnabled() {
        return !llmKey.isEmpty() || !apiKey.isEmpty();
    }

    /** Returns the embedding vector for the text, or null when Gemini is not enabled. */
    public float[] embed(String text) {
        if (apiKey.isEmpty()) return null;   // embeddings need the Gemini key; without it the course search uses the keyword fallback
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text)))));
            JsonNode root = post(embeddingModel + ":embedContent", body);
            JsonNode values = root.path("embedding").path("values");
            if (!values.isArray() || values.isEmpty()) {
                throw new ApiCommunicationException("Gemini returned no embedding", null);
            }
            float[] vec = new float[values.size()];
            for (int i = 0; i < vec.length; i++) vec[i] = (float) values.get(i).asDouble();
            return vec;
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiCommunicationException("Gemini embedding request failed", e);
        }
    }

    /** Returns Gemini-generated text, or null when Gemini is not enabled. */
    public String generate(String prompt) {
        if (!isEnabled()) return null;
        if (!llmKey.isEmpty()) return generateWithLlm(prompt);
        try {
            Map<String, Object> request = new java.util.LinkedHashMap<>();
            request.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            if (generationModel.contains("2.5-flash")) {
                // 2.5 Flash "thinks" before it answers, which can take longer than our timeout; a quiz does not need it
                request.put("generationConfig", Map.of("thinkingConfig", Map.of("thinkingBudget", 0)));
            }
            String body = mapper.writeValueAsString(request);
            JsonNode root = post(generationModel + ":generateContent", body);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini generation request failed: {}", e.toString());
            throw new ApiCommunicationException("Gemini generation request failed", e);
        }
    }

    /** Text generation through an OpenAI-compatible "chat/completions" API (Groq, OpenRouter, ...). */
    private String generateWithLlm(String prompt) {
        try {
            consumeBudget();
            String body = mapper.writeValueAsString(Map.of(
                    "model", llmModel,
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", prompt))));
            HttpRequest req = HttpRequest.newBuilder(URI.create(llmBase + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                String text = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
                log.warn("LLM {} answered {}: {}", llmBase, res.statusCode(), text.length() > 400 ? text.substring(0, 400) : text);
                throw new ApiCommunicationException("LLM API responded with status " + res.statusCode(), null);
            }
            String content = mapper.readTree(res.body()).path("choices").path(0).path("message").path("content").asText(null);
            return content == null ? null : content.replaceAll("(?s)<think>.*?</think>", "").trim();   // some models print their reasoning in <think> tags
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiCommunicationException("LLM request was interrupted", e);
        } catch (Exception e) {
            log.warn("LLM request failed: {}", e.toString());
            throw new ApiCommunicationException("LLM request failed", e);
        }
    }

    /** Caps how many Gemini calls the whole app makes per day, whatever the users do. */
    private void consumeBudget() {
        LocalDate today = LocalDate.now();
        if (!today.equals(budgetDay)) {
            synchronized (this) {
                if (!today.equals(budgetDay)) {
                    budgetDay = today;
                    used.set(0);
                }
            }
        }
        if (used.incrementAndGet() > dailyBudget) {
            throw new ApiCommunicationException("Daily AI call budget exhausted", null);
        }
    }

    private JsonNode post(String path, String json) throws Exception {
        consumeBudget();
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            // the real reason (for example "API key not valid") goes to the log; the key itself is never logged
            String body = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
            log.warn("Gemini {} answered {}: {}", path, res.statusCode(), body.length() > 400 ? body.substring(0, 400) : body);
            throw new ApiCommunicationException("Gemini API responded with status " + res.statusCode(), null);
        }
        return mapper.readTree(res.body());
    }
}