package com.example.chat.service;

import com.example.chat.util.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * AI回复逻辑
 */
public class OpenAiChatClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = JsonUtil.gson();
    private final String apiKey;
    private final String completionsUrl;
    private final String model;

    public OpenAiChatClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.completionsUrl = normalizeBaseUrl(baseUrl);
        this.model = model;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    // 核心逻辑
    public String chat(List<ChatMessage> messages) {
        if (!isEnabled()) {
            throw new IllegalStateException("OpenAI client is disabled because apiKey is missing");
        }
        Objects.requireNonNull(messages, "messages");

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);

        JsonArray array = new JsonArray();
        for (ChatMessage message : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            item.addProperty("content", message.content());
            array.add(item);
        }
        payload.add("messages", array);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(completionsUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("OpenAI API error: http " + response.statusCode() + " body: " + response.body());
            }
            JsonObject body = gson.fromJson(response.body(), JsonObject.class);
            JsonArray choices = body.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("OpenAI API returned no choices");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                throw new IllegalStateException("OpenAI API response missing content");
            }
            return message.get("content").getAsString();
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI chat call failed: " + e.getMessage(), e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String sanitized = baseUrl == null || baseUrl.isBlank()
                ? "https://api.openai.com"
                : baseUrl;
        if (sanitized.endsWith("/v1/chat/completions")) {
            return sanitized;
        }
        if (sanitized.endsWith("/")) {
            return sanitized + "v1/chat/completions";
        }
        return sanitized + "/v1/chat/completions";
    }

    public record ChatMessage(String role, String content) {
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }

        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
    }
}
