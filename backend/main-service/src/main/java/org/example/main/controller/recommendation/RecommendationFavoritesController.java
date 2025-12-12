package org.example.main.controller.recommendation;

import lombok.RequiredArgsConstructor;
import org.example.main.dto.response.recommendation.FavoriteRecommendationResponseDto;
import org.example.main.model.recommendation.FavoriteRecommendation;
import org.example.main.repository.recommendation.FavoriteRecommendationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simple controller to persist favorite recommendations.
 *
 * Endpoints:
 *  - GET  /api/recommendations/favorites           -> list all favorites (optionally ?mine=true)
 *  - POST /api/recommendations/favorites           -> add favorite (request body uses FavoriteRecommendationResponseDto-ish payload)
 *  - DELETE /api/recommendations/favorites/{id}    -> remove favorite by id
 *
 * Note: authentication integration (resolving current user) is left minimal — adapt to your security setup.
 */
@RestController
@RequestMapping("/api/recommendations/favorites")
@RequiredArgsConstructor
public class RecommendationFavoritesController {

    private final FavoriteRecommendationRepository repository;

    
    private FavoriteRecommendationResponseDto toDto(FavoriteRecommendation e) {
        return FavoriteRecommendationResponseDto.builder()
                .id(e.getId())
                .ingredients(e.getIngredients() != null ? List.of(e.getIngredients().split(",")) : null)
                .description(e.getDescription())
                .menuItemId(e.getMenuItemId())
                .menuItemName(e.getMenuItemName())
                .calories(e.getCalories())
                .protein(e.getProtein())
                .fats(e.getFats())
                .carbs(e.getCarbs())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    @GetMapping
    public ResponseEntity<List<FavoriteRecommendationResponseDto>> listFavorites(@RequestParam(name = "mine", required = false) Boolean mine) {
        List<FavoriteRecommendation> list = repository.findAll();
        List<FavoriteRecommendationResponseDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<FavoriteRecommendationResponseDto> addFavorite(@Valid @RequestBody FavoriteRecommendationResponseDto req) {
        FavoriteRecommendation e = FavoriteRecommendation.builder()
                .menuItemId(req.getMenuItemId())
                .menuItemName(req.getMenuItemName())
                .description(req.getDescription())
                .ingredients(req.getIngredients() != null ? String.join(",", req.getIngredients()) : null)
                .calories(req.getCalories())
                .protein(req.getProtein())
                .fats(req.getFats())
                .carbs(req.getCarbs())
                .createdBy(null)
                .createdAt(OffsetDateTime.now())
                .build();
        repository.save(e);
        return ResponseEntity.ok(toDto(e));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFavorite(@PathVariable("id") UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}