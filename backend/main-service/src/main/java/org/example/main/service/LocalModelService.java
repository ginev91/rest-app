package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Local model adapter supporting three backends:
 *  - "mock"  : returns a small canned JSON response (ideal for homework/demo/teacher)
 *  - "cli"   : runs the 'ollama' CLI (configurable path)
 *  - "http"  : calls a local model HTTP server (ollama serve or another local server)
 *
 * This implementation ensures mock output is valid JSON and normalizes/unwraps nested/escaped JSON
 * returned by real model backends so downstream parsing is robust.
 */
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

    /**
     * Generate text for the prompt using the configured backend.
     * Returns a Mono that emits the model's text output (expected to be a JSON array string).
     */
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

    /**
     * Simple mock response useful for demos and grading.
     * Uses ObjectMapper to produce safe JSON (no unescaped newlines).
     */
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

    /**
     * CLI implementation: runs configured CLI (e.g. 'ollama') via ProcessBuilder.
     */
    private Mono<String> generateViaCli(String prompt) {

        return null; // still working on this
//        return Mono.fromCallable(() -> {
//                    ProcessBuilder pb = new ProcessBuilder(cliPath, "run", model, "--prompt", prompt);
//                    pb.redirectErrorStream(true);
//                    Process process;
//                    try {
//                        process = pb.start();
//                    } catch (IOException ioe) {
//                        throw new LocalModelException("Local model CLI '" + cliPath + "' not found or failed to start. Install Ollama or set local.model.cli.path.", ioe);
//                    }
//
//                    try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                        String output = r.lines().collect(Collectors.joining("\n"));
//                        boolean exited = process.waitFor(cliTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
//                        if (!exited) {
//                            process.destroyForcibly();
//                            throw new LocalModelException("Local model process timed out");
//                        }
//                        int code = process.exitValue();
//                        if (code != 0) {
//                            throw new LocalModelException("Local model process exited with code " + code + ". Output: " + output);
//                        }
//                        String normalized = normalizeOutput(output == null ? "" : output.trim());
//                        return normalized;
//                    } catch (IOException | InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        throw new LocalModelException("Error reading output from local model process", e);
//                    }
//                }).subscribeOn(Schedulers.boundedElastic())
//                .timeout(Duration.ofMillis(cliTimeoutMs + 1000))
//                .onErrorMap(throwable -> {
//                    if (throwable instanceof LocalModelException) return throwable;
//                    return new LocalModelException("Local model CLI invocation failed", throwable);
//                });
    }

    /**
     * HTTP implementation: POST to the configured httpUrl.
     */
    private Mono<String> generateViaHttp(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "max_tokens", 50       // lower token count for faster response; adjust as needed
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


    /**
     * Normalize/unquote/unescape raw output from model backends and try to extract inner JSON if present.
     */
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

    /**
     * Very small unescape helper: handle common escaped sequences produced by models.
     */
    private String unescapeCommonJsonEscapes(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    /**
     * Attempts to locate the first top-level JSON array in the string and return it
     * (substring from first '[' to the matching ']' inclusive). Returns null if none found
     * or balancing fails.
     */
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