package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.dto.request.MealRecommendationRequestDto;
import org.example.main.dto.response.MealRecommendationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stateless single-turn meal recommendation service.
 *
 * Improvements made:
 * - More robust parsing of the model output: strips common wrappers (code fences),
 *   extracts the first JSON array substring if the model emits extra text around the JSON,
 *   and only then attempts to parse. Falls back to trying to parse a single JSON object,
 *   and finally falls back to returning a DTO with the raw text in description.
 *
 * This makes the service tolerant to local-model outputs that include explanatory text
 * or accidentally echo the prompt while still returning structured data when present.
 */
@Service
public class MealRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(MealRecommendationService.class);

    private final LocalModelService localModelService;
    private final ObjectMapper objectMapper;
    private final String modelName;

    public MealRecommendationService(LocalModelService localModelService,
                                     ObjectMapper objectMapper,
                                     @Value("${local.model.name:llama3}") String modelName) {
        this.localModelService = localModelService;
        this.objectMapper = objectMapper;
        this.modelName = modelName;
    }

    /**
     * Main entrypoint: single-input -> list-of-recommendations.
     */
    public Mono<List<MealRecommendationResponseDto>> recommend(MealRecommendationRequestDto request) {
        String prompt = defaultPrompt(request);

        return localModelService.generate(prompt)
                .map(raw -> {
                    if (raw == null) {
                        log.warn("Local model returned null");
                        return List.<MealRecommendationResponseDto>of();
                    }

                    String cleaned = stripCodeFences(raw).trim();

                    String maybeArray = extractFirstJsonArray(cleaned);

                    if (maybeArray != null) {
                        try {
                            List parsed = objectMapper.readValue(maybeArray, List.class);
                            List<MealRecommendationResponseDto> result = new ArrayList<>();
                            for (Object o : parsed) {
                                MealRecommendationResponseDto dto = objectMapper.convertValue(o, MealRecommendationResponseDto.class);
                                result.add(dto);
                            }
                            log.debug("Parsed {} recommendation(s) from model JSON array.", result.size());
                            return result;
                        } catch (Exception ex) {
                            log.warn("Found JSON-array-like substring but failed to parse it. substring={} error={}", maybeArray, ex.getMessage());
                            // fall through to other parsing attempts
                        }
                    }

                    // Try to parse the whole cleaned text as a JSON array
                    try {
                        List parsed = objectMapper.readValue(cleaned, List.class);
                        List<MealRecommendationResponseDto> result = new ArrayList<>();
                        for (Object o : parsed) {
                            MealRecommendationResponseDto dto = objectMapper.convertValue(o, MealRecommendationResponseDto.class);
                            result.add(dto);
                        }
                        log.debug("Parsed {} recommendation(s) from model output (whole-body parse).", result.size());
                        return result;
                    } catch (Exception ex) {
                        log.debug("Whole-body parse as JSON array failed: {}", ex.getMessage());
                    }

                    // Try parse as a single JSON object (map -> dto)
                    try {
                        Map parsedMap = objectMapper.readValue(cleaned, Map.class);
                        MealRecommendationResponseDto dto = objectMapper.convertValue(parsedMap, MealRecommendationResponseDto.class);
                        log.debug("Parsed single recommendation object from model output.");
                        return List.of(dto);
                    } catch (Exception ex) {
                        // final fallback: return a single DTO with raw text in description
                        log.warn("Failed to parse model output as JSON array/object, returning raw text. Raw output: {}", raw);
                        MealRecommendationResponseDto fallback = new MealRecommendationResponseDto();
                        fallback.setDescription(raw == null ? "No response" : raw);
                        return List.of(fallback);
                    }
                });
    }

    /**
     * Removes common markdown/code-fence wrappers from model output.
     * Example transformations:
     *  - "```json\n[...]\n```" -> "[...]"
     *  - "``` \n[...] \n```" -> "[...]"
     */
    private String stripCodeFences(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        // Remove triple backtick code fences with optional language hint
        if (trimmed.startsWith("```")) {
            int end = trimmed.lastIndexOf("```");
            if (end > 3) {
                String inner = trimmed.substring(trimmed.indexOf('\n') + 1, end).trim();
                return inner;
            }
        }
        // Also remove single leading/trailing backticks if present
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
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
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    // found matching closing bracket
                    return s.substring(start, i + 1);
                }
            }
        }
        // no balanced closing bracket found
        return null;
    }

    /**
     * Compose a prompt for the local model from the request.
     * If the request contains structured fields in the DTO, they can be appended here.
     * For now we primarily use request.getPrompt() (free-text).
     */
    private String defaultPrompt(MealRecommendationRequestDto request) {
        String userPrompt = request == null ? "" : (request.getPrompt() == null ? "" : request.getPrompt().trim());

        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful nutrition assistant. ");
        sb.append("Given the user's request below, return ONLY a JSON array of recommendation objects and nothing else. ");
        sb.append("Do NOT include any explanation, prompt echo, or surrounding text; do NOT include markdown or code fences. ");
        sb.append("Each object may include fields such as recipe, description, matchedMenuItemId, menuItemId, menuItemName, score, matchPercentage, calories, protein, fats, carbs, ingredients. ");
        sb.append("If you must make assumptions (e.g. about allergies or calorie targets), include an 'assumptions' field in the first object. ");
        sb.append("Keep the array length to at most 5. Example:\n");
        sb.append("[{ \"recipe\":\"...\",\"description\":\"...\",\"menuItemName\":\"Grilled Salmon\",\"score\":0.95,\"calories\":550,\"protein\":40 }]\n\n");
        sb.append("User request: ").append(userPrompt);
        return sb.toString();
    }
}