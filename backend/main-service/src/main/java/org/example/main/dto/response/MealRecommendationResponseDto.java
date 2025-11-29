package org.example.main.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response from AI: structured meal suggestions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealRecommendationResponseDto {
    private List<MealSuggestion> suggestions;
    private String rawAiText;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealSuggestion {
        private String name;
        private List<String> ingredients;
        private String instructions;
        private Integer calories;
        private Integer totalTimeMinutes;

        private Map<String, String> substitutes;
    }
}