package com.ankkun.ecommerce.module.ai.service;

import com.ankkun.ecommerce.common.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${ai.sidecar-url:http://localhost:8000}")
    private String sidecarUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Object chatCompletion(String userId, String message, List<Object> chatHistory) {
        try {
            var body = objectMapper.writeValueAsString(Map.of(
                    "message", message,
                    "chat_history", chatHistory != null ? chatHistory : List.of(),
                    "user_id", userId,
                    "stream", false
            ));

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(sidecarUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), Object.class);

        } catch (Exception e) {
            log.error("[ChatService] AI Sidecar error: {}", e.getMessage());
            throw new ServiceUnavailableException("Hệ thống Chatbot AI hiện không phản hồi. Vui lòng thử lại sau.");
        }
    }
}
