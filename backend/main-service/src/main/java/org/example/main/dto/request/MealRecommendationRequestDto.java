package org.example.main.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Simple request DTO that matches the frontend:
 * export interface RecommendationRequest { prompt: string }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealRecommendationRequestDto {
    @NotBlank
    private String prompt;
}