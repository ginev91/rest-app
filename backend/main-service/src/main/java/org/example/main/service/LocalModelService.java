package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.exception.LocalModelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LocalModelService {

    private final String backend;
    private final String model;
    private final String cliPath;
    private final long cliTimeoutMs;
    private final WebClient webClient;
    private final String httpUrl;
    private final Duration httpTimeout;
    private final ObjectMapper objectMapper;

    public LocalModelService(
            @Value("${local.model.backend:mock}") String backend,
            @Value("${local.model.name:llama3.2}") String model,
            @Value("${local.model.cli.path:ollama}") String cliPath,
            @Value("${local.model.cli.timeout-ms:120000}") long cliTimeoutMs,
            WebClient.Builder webClientBuilder,
            @Value("${local.model.http.url:http://localhost:11434/api/generate}") String httpUrl,
            @Value("${local.model.http.timeout-ms:120000}") long httpTimeoutMs,
            ObjectMapper objectMapper) {
        this.backend = backend;
        this.model = model;
        this.cliPath = cliPath;
        this.cliTimeoutMs = cliTimeoutMs;
        this.webClient = webClientBuilder.build();
        this.httpUrl = httpUrl;
        this.httpTimeout = Duration.ofMillis(httpTimeoutMs);
        this.objectMapper = objectMapper;
    }


    public Mono<String> generate(String prompt) {
        switch (backend.toLowerCase()) {
            case "http":
                return generateViaHttp(prompt);
            case "cli":
                return generateViaCli(prompt);
            case "mock":
            default:
                return generateMock(prompt);
        }
    }

    private Mono<String> generateMock(String prompt) {
        try {
            Map<String, Object> rec = Map.of(
                    "menuItemName", "Simple Grilled Chicken Salad",
                    "description", "High-protein salad with grilled chicken, mixed greens and vinaigrette.",
                    "calories", 750,
                    "protein", 48,
                    "carbs", 45,
                    "fats", 30,
                    "ingredients", List.of("chicken breast","mixed greens","tomatoes","cucumber","olive oil","lemon","salt","pepper")
            );

            List<Object> out = List.of(rec);
            String json = objectMapper.writeValueAsString(out);
            return Mono.just(json);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private Mono<String> generateViaCli(String prompt) {

        return null; 









//























    }

    private Mono<String> generateViaHttp(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "max_tokens", 50       
        );

        return webClient.post()
                .uri("http://localhost:11434/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(chunk -> chunk.get("response"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .map(this::normalizeOutput)
                .defaultIfEmpty("No response from model")
                .timeout(httpTimeout)
                .onErrorMap(err -> new LocalModelException(
                        "Local model HTTP backend failed: " + err.getMessage(), err));
    }


    private String normalizeOutput(String raw) {

        if (raw == null) return "";

        String trimmed = raw.trim();

        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            try {
                String unquoted = objectMapper.readValue(trimmed, String.class).trim();
                if (!unquoted.isEmpty()) trimmed = unquoted;
            } catch (Exception ignored) {
            }
        }

        if (trimmed.startsWith("{")) {
            try {
                Map<?, ?> map = objectMapper.readValue(trimmed, Map.class);
                Object text = map.get("text");
                if (text instanceof String) {
                    String inner = ((String) text).trim();
                    if ((inner.startsWith("\"") && inner.endsWith("\""))) {
                        try {
                            String unquotedInner = objectMapper.readValue(inner, String.class).trim();
                            inner = unquotedInner;
                        } catch (Exception ignored) {}
                    }
                    if (inner.startsWith("[") || inner.startsWith("{")) {
                        return inner;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String unescaped = unescapeCommonJsonEscapes(trimmed);
        String maybeArray = extractFirstJsonArray(unescaped);
        if (maybeArray != null) {
            return maybeArray;
        }

        String directArray = extractFirstJsonArray(trimmed);
        if (directArray != null) return directArray;

        return trimmed;
    }

    private String unescapeCommonJsonEscapes(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private String extractFirstJsonArray(String s) {
        if (s == null) return null;
        int start = s.indexOf('[');
        if (start < 0) return null;
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) continue;
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}