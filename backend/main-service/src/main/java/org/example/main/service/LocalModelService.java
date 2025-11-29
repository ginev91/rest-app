package org.example.main.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple adapter to run a local model. Supports two backends:
 * - "cli": runs the installed 'ollama' CLI via ProcessBuilder
 * - "http": calls a local model HTTP endpoint (e.g. Ollama server or other local server)
 *
 * Configure in application.yml under local.model.*
 */
@Service
public class LocalModelService {

    private final String backend;        // "cli" or "http"
    private final String model;          // model name used by CLI or HTTP
    private final long cliTimeoutMs;     // timeout for CLI
    private final WebClient webClient;   // used for HTTP backend
    private final String httpUrl;        // full URL for HTTP completion endpoint
    private final Duration httpTimeout;

    public LocalModelService(
            @Value("${local.model.backend:cli}") String backend,
            @Value("${local.model.name:llama3}") String model,
            @Value("${local.model.cli.timeout-ms:30000}") long cliTimeoutMs,
            WebClient.Builder webClientBuilder,
            @Value("${local.model.http.url:http://localhost:11434/v1/generate}") String httpUrl,
            @Value("${local.model.http.timeout-ms:30000}") long httpTimeoutMs) {
        this.backend = backend;
        this.model = model;
        this.cliTimeoutMs = cliTimeoutMs;
        this.webClient = webClientBuilder.build();
        this.httpUrl = httpUrl;
        this.httpTimeout = Duration.ofMillis(httpTimeoutMs);
    }

    /**
     * Generate text for the prompt using the configured backend.
     * Returns a Mono that emits the model's text output.
     */
    public Mono<String> generate(String prompt) {
        if ("http".equalsIgnoreCase(backend)) {
            return generateViaHttp(prompt);
        } else {
            return generateViaCli(prompt);
        }
    }

    /**
     * CLI implementation: runs `ollama run <model> --prompt "<prompt>"`
     * This runs on boundedElastic to avoid blocking Netty/io threads.
     */
    private Mono<String> generateViaCli(String prompt) {
        return Mono.fromCallable(() -> {
                    // Build the CLI command. Adjust flags if needed for your model/ollama version.
                    ProcessBuilder pb = new ProcessBuilder("ollama", "run", model, "--prompt", prompt);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    // Read stdout (merged with stderr).
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        // Read until EOF or timeout (we will waitFor with timeout below).
                        String output = r.lines().collect(Collectors.joining("\n"));
                        boolean exited = process.waitFor(cliTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (!exited) {
                            process.destroyForcibly();
                            throw new IllegalStateException("Model process timed out");
                        }
                        int code = process.exitValue();
                        if (code != 0) {
                            throw new IllegalStateException("Model process exited with code " + code + ". Output: " + output);
                        }
                        return output == null ? "" : output.trim();
                    }
                }).subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMillis(cliTimeoutMs + 1000));
    }

    /**
     * HTTP implementation: POST to the configured httpUrl.
     * The expected body is a JSON with { "model": "...", "prompt": "..." } by default.
     * Adjust to the exact API your local server exposes (Ollama HTTP may differ; modify accordingly).
     */
    private Mono<String> generateViaHttp(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt
        );

        return webClient.post()
                .uri(httpUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // Try common shapes: response.text, response.output, or flatten choices
                    Object text = ((Map) response).get("text");
                    if (text != null) return text.toString();
                    Object output = ((Map) response).get("output");
                    if (output != null) return output.toString();
                    Object choices = ((Map) response).get("choices");
                    if (choices instanceof Iterable) {
                        for (Object c : (Iterable) choices) {
                            if (c instanceof Map) {
                                Object msg = ((Map) c).get("text");
                                if (msg != null) return msg.toString();
                            }
                        }
                    }
                    return response.toString();
                })
                .timeout(httpTimeout);
    }
}