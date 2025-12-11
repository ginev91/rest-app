package org.example.main.controller.recommendation;

import org.example.main.dto.request.recommendation.MealRecommendationRequestDto;
import org.example.main.dto.response.recommendation.MealRecommendationResponseDto;
import org.example.main.service.recommendation.MealRecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealRecommendationControllerTest {

    @Mock
    MealRecommendationService mealRecommendationService;

    @Test
    void recommend_returnsList_whenServiceProvidesResult() {
        MealRecommendationController ctrl = new MealRecommendationController(mealRecommendationService);

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("recommend something");

        MealRecommendationResponseDto r1 = new MealRecommendationResponseDto();
        r1.setMenuItemName("Dish A");
        MealRecommendationResponseDto r2 = new MealRecommendationResponseDto();
        r2.setMenuItemName("Dish B");

        List<MealRecommendationResponseDto> expected = List.of(r1, r2);

        when(mealRecommendationService.recommend(req)).thenReturn(Mono.just(expected));

        List<MealRecommendationResponseDto> result = ctrl.recommend(req);

        assertThat(result).isEqualTo(expected);
        verify(mealRecommendationService).recommend(req);
    }

    @Test
    void recommend_returnsEmptyList_whenServiceReturnsEmptyMono() {
        MealRecommendationController ctrl = new MealRecommendationController(mealRecommendationService);

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("  ");

        when(mealRecommendationService.recommend(req)).thenReturn(Mono.empty());

        List<MealRecommendationResponseDto> result = ctrl.recommend(req);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(mealRecommendationService).recommend(req);
    }

    @Test
    void recommend_handlesException_and_returnsEmptyList() {
        MealRecommendationController ctrl = new MealRecommendationController(mealRecommendationService);

        MealRecommendationRequestDto req = new MealRecommendationRequestDto();
        req.setPrompt("anything");

        when(mealRecommendationService.recommend(req)).thenReturn(Mono.error(new RuntimeException("boom")));

        List<MealRecommendationResponseDto> result = ctrl.recommend(req);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(mealRecommendationService).recommend(req);
    }

    @Test
    void recommend_acceptsNullRequest_and_delegatesToService() {
        MealRecommendationController ctrl = new MealRecommendationController(mealRecommendationService);

        MealRecommendationResponseDto r = new MealRecommendationResponseDto();
        r.setMenuItemName("Dish X");

        when(mealRecommendationService.recommend(null)).thenReturn(Mono.just(List.of(r)));

        List<MealRecommendationResponseDto> result = ctrl.recommend(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMenuItemName()).isEqualTo("Dish X");
        verify(mealRecommendationService).recommend(null);
    }
}