package com.examly.springapp.service;

import com.examly.springapp.exception.ApiCommunicationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thin client for the Gemini embedding and generation API with a daily call budget.
 *
 * @author Sumit
 */
@Service
public class GeminiService {
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String embeddingModel;
    private final String generationModel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int dailyBudget;
    private final AtomicInteger used = new AtomicInteger();
    private volatile LocalDate budgetDay = LocalDate.now();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public GeminiService(@Value("${gemini.api.key:}") String apiKey,
                         @Value("${gemini.embedding.model:text-embedding-004}") String embeddingModel,
                         @Value("${gemini.generation.model:gemini-2.5-flash}") String generationModel,
                         @Value("${ai.daily-embedding-budget:2000}") int dailyBudget) {
        this.dailyBudget = dailyBudget;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.embeddingModel = embeddingModel;
        this.generationModel = generationModel;
    }

    /** True only when a non-empty gemini.api.key is configured. */
    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    /** Returns the embedding vector for the text, or null when Gemini is not enabled. */
    public float[] embed(String text) {
        if (!isEnabled()) return null;
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
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))));
            JsonNode root = post(generationModel + ":generateContent", body);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
        } catch (ApiCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiCommunicationException("Gemini generation request failed", e);
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
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new ApiCommunicationException("Gemini API responded with status " + res.statusCode(), null);
        }
        return mapper.readTree(res.body());
    }
}
