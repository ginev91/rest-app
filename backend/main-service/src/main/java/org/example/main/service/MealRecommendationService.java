package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.dto.request.MealRecommendationRequestDto;
import org.example.main.dto.response.MealRecommendationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class MealRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(MealRecommendationService.class);

    private final LocalModelService localModelService;
    private final ObjectMapper objectMapper;

    public MealRecommendationService(LocalModelService localModelService,
                                     ObjectMapper objectMapper) {
        this.localModelService = localModelService;
        this.objectMapper = objectMapper;
    }

    /**
     * Main entrypoint: single-input -> list-of-recommendations.
     */
    public Mono<List<MealRecommendationResponseDto>> recommend(MealRecommendationRequestDto request) {
        String prompt = defaultPrompt(request);

        return localModelService.generate(prompt)
                .map(raw -> {
                    if (raw == null || raw.isBlank()) return List.<MealRecommendationResponseDto>of();

                    String cleaned = stripCodeFences(raw.trim());
                    cleaned = stripQuotes(cleaned);

                    try {
                        List<?> parsed = objectMapper.readValue(cleaned, List.class);
                        List<MealRecommendationResponseDto> result = new ArrayList<>();
                        for (Object o : parsed) {
                            MealRecommendationResponseDto dto = objectMapper.convertValue(o, MealRecommendationResponseDto.class);
                            result.add(dto);
                        }
                        log.debug("Parsed {} recommendation(s) from model output.", result.size());
                        return result;
                    } catch (Exception ex) {
                        log.warn("Failed to parse model output as JSON array: {}", ex.getMessage());
                        return List.<MealRecommendationResponseDto>of();
                    }
                });
    }

    /**
     * Removes common markdown/code-fence wrappers from model output.
     */
    private String stripCodeFences(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int end = trimmed.lastIndexOf("```");
            if (end > 3) {
                String inner = trimmed.substring(trimmed.indexOf('\n') + 1, end).trim();
                return inner;
            }
        }
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * Remove wrapping quotes if JSON is double-encoded.
     */
    private String stripQuotes(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            try {
                return objectMapper.readValue(s, String.class).trim();
            } catch (Exception ignored) {}
        }
        return s;
    }

    /**
     * Compose a prompt for the local model from the request.
     */
    private String defaultPrompt(MealRecommendationRequestDto request) {
        String userPrompt = request == null || request.getPrompt() == null ? "" : request.getPrompt().trim();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful nutrition assistant. ");
        sb.append("Given the user's request below, return ONLY a JSON array of recommendation objects and nothing else. ");
        sb.append("Do NOT include any explanation, prompt echo, or surrounding text; do NOT include markdown or code fences. ");
        sb.append("Each object may include fields such as recipe, description, matchedMenuItemId, menuItemId, menuItemName, score, matchPercentage, calories, protein, fats, carbs, ingredients. ");
        sb.append("If you must make assumptions (e.g. about allergies or calorie targets), include an 'assumptions' field in the first object. ");
        sb.append("Keep the array length to at most 4. Example:\n");
        sb.append("[{ \"recipe\":\"...\",\"description\":\"...\",\"menuItemName\":\"Grilled Salmon\",\"score\":0.95,\"calories\":550,\"protein\":40,\"fats\":23,\"carbs:75 }]\n\n");
        sb.append("User request: ").append(userPrompt);
        return sb.toString();
    }
}
