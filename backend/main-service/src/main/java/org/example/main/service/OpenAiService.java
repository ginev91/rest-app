package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final WebClient webClient;
    private final Duration timeout;
    private final ObjectMapper objectMapper;

    public OpenAiService(WebClient.Builder webClientBuilder,
                         @Value("${openai.api.base-url:https://api.openai.com}") String baseUrl,
                         @Value("${openai.api.key}") String apiKey,
                         @Value("${openai.api.timeout-ms:15000}") long timeoutMs,
                         ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.timeout = Duration.ofMillis(timeoutMs);
        this.objectMapper = objectMapper;
    }

    /**
     * Calls OpenAI chat completions and returns the raw assistant text.
     */
    public String chat(String systemPrompt, List<Map<String, String>> messages, String model, Double temperature) {
        if (model == null || model.isBlank()) model = "gpt-3.5-turbo";
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature
        );

        Mono<Map> resp = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(timeout);

        Map result = resp.block(timeout);
        if (result == null) throw new IllegalStateException("Empty response from OpenAI");

        Object choicesObj = result.get("choices");
        if (!(choicesObj instanceof List) || ((List) choicesObj).isEmpty()) {
            throw new IllegalStateException("No choices returned from OpenAI: " + result);
        }
        Object first = ((List) choicesObj).get(0);
        if (!(first instanceof Map)) throw new IllegalStateException("Unexpected choice format: " + first);
        Map firstMap = (Map) first;
        Object messageObj = firstMap.get("message");
        if (messageObj instanceof Map) {
            Object content = ((Map) messageObj).get("content");
            return content == null ? "" : content.toString().trim();
        }
        Object text = firstMap.get("text");
        return text == null ? "" : text.toString();
    }

    /**
     * Convenience wrapper for asking for JSON and parsing it into a Map.
     */
    public Map<String, Object> chatJson(List<Map<String, String>> messages, String model, Double temperature) throws Exception {
        String text = chat(null, messages, model, temperature);
        // Try to parse response as JSON
        try {
            return objectMapper.readValue(text, Map.class);
        } catch (Exception ex) {
            // If parsing fails, throw with helpful message (caller may fallback to raw text)
            throw new IllegalStateException("Failed to parse OpenAI response as JSON. Raw response: " + text, ex);
        }
    }
}