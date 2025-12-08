package org.example.main.controller;

import org.example.main.dto.request.MealRecommendationRequestDto;
import org.example.main.dto.response.MealRecommendationResponseDto;
import org.example.main.service.MealRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class MealRecommendationController {

    private static final Logger log = LoggerFactory.getLogger(MealRecommendationController.class);

    private final MealRecommendationService mealRecommendationService;

    public MealRecommendationController(MealRecommendationService mealRecommendationService) {
        this.mealRecommendationService = mealRecommendationService;
    }

    @PostMapping(value = "/recommendations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MealRecommendationResponseDto> recommend(@RequestBody @Valid MealRecommendationRequestDto request) {
        String prompt = request == null ? "" : (request.getPrompt() == null ? "" : request.getPrompt().trim());
        log.info("Received meal recommendation prompt (blocking): {}", prompt);

        
        
        try {
            List<MealRecommendationResponseDto> result = mealRecommendationService
                    .recommend(request)
                    .block(Duration.ofSeconds(120));
            return result == null ? Collections.emptyList() : result;
        } catch (Exception ex) {
            log.error("Error while generating recommendation (blocking): {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}