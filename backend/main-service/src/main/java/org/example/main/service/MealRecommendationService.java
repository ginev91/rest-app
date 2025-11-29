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
import java.util.Map;

/**
 * Stateless single-turn meal recommendation service.
 * - Accepts a MealRecommendationRequestDto (single prompt string or optional structured fields).
 * - Uses LocalModelService.generate(prompt) to produce model output (expected to be a JSON array).
 * - Attempts to parse the output into a List<MealRecommendationResponseDto>.
 * - If parsing fails, returns a single-item list with the raw model text placed into the description field
 *   of a MealRecommendationResponseDto so the frontend receives something displayable.
 *
 * Keep filename exactly: MealRecommendationService.java
 */
@Service
public class MealRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(MealRecommendationService.class);

    private final LocalModelService localModelService;
    private final ObjectMapper objectMapper;
    private final String modelName;

    public MealRecommendationService(LocalModelService localModelService,
                                     ObjectMapper objectMapper,
                                     // model name is optional here; primarily used by LocalModelService config
                                     org.springframework.beans.factory.annotation.Value("${local.model.name:llama3}") String modelName) {
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
                    try {
                        // Expecting the model to return a JSON array of objects matching MealRecommendationResponseDto
                        List parsed = objectMapper.readValue(raw, List.class);
                        List<MealRecommendationResponseDto> result = new ArrayList<>();
                        for (Object o : parsed) {
                            MealRecommendationResponseDto dto = objectMapper.convertValue(o, MealRecommendationResponseDto.class);
                            result.add(dto);
                        }
                        return result;
                    } catch (Exception ex) {
                        // Parsing failed -> return single-item list with raw text in description
                        log.warn("Failed to parse model output as JSON array, returning raw text. Raw output: {}", raw);
                        MealRecommendationResponseDto fallback = new MealRecommendationResponseDto();
                        // try to set a couple of common fields if present, otherwise place raw text in description
                        try {
                            Map parsedMap = objectMapper.readValue(raw, Map.class);
                            MealRecommendationResponseDto dto = objectMapper.convertValue(parsedMap, MealRecommendationResponseDto.class);
                            return List.of(dto);
                        } catch (Exception ex2) {
                            fallback.setDescription(raw == null ? "No response" : raw);
                            return List.of(fallback);
                        }
                    }
                });
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
        sb.append("Given the user's request below, return ONLY a JSON array of recommendation objects. ");
        sb.append("Each object may include fields such as recipe, description, matchedMenuItemId, menuItemId, menuItemName, score, matchPercentage, calories, protein. ");
        sb.append("If you must make assumptions (e.g. about allergies or calorie targets), include an 'assumptions' field in the first object. ");
        sb.append("Keep the array length to at most 5. Example:\n");
        sb.append("[{ \"recipe\":\"...\",\"description\":\"...\",\"menuItemName\":\"Grilled Salmon\",\"score\":0.95,\"matchPercentage\":95,\"calories\":550,\"protein\":40 }]\n\n");
        sb.append("User request: ").append(userPrompt);
        return sb.toString();
    }
}