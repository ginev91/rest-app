package org.example.main.dto.response;

import org.example.main.dto.response.recommendation.MealRecommendationResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


class MealRecommendationResponseDtoTest {

    @Test
    void builder_getters_setters_equals_hashcode_toString() {
        MealRecommendationResponseDto dto = MealRecommendationResponseDto.builder()
                .ingredients(List.of("tomato", "cheese"))
                .description("Tasty meal")
                .matchedMenuItemId("match-1")
                .menuItemId("m-1")
                .menuItemName("Margherita")
                .calories(450)
                .protein(20)
                .fats(15)
                .carbs(55)
                .assumptions(List.of("no-gluten"))
                .build();

        
        assertThat(dto.getIngredients()).containsExactly("tomato", "cheese");
        assertThat(dto.getDescription()).isEqualTo("Tasty meal");
        assertThat(dto.getMatchedMenuItemId()).isEqualTo("match-1");
        assertThat(dto.getMenuItemId()).isEqualTo("m-1");
        assertThat(dto.getMenuItemName()).isEqualTo("Margherita");
        assertThat(dto.getCalories()).isEqualTo(450);
        assertThat(dto.getProtein()).isEqualTo(20);
        assertThat(dto.getFats()).isEqualTo(15);
        assertThat(dto.getCarbs()).isEqualTo(55);
        assertThat(dto.getAssumptions()).containsExactly("no-gluten");

        
        assertThat(dto.toString()).contains("Margherita", "Tasty meal");

        
        MealRecommendationResponseDto dto2 = MealRecommendationResponseDto.builder()
                .ingredients(List.of("tomato", "cheese"))
                .description("Tasty meal")
                .matchedMenuItemId("match-1")
                .menuItemId("m-1")
                .menuItemName("Margherita")
                .calories(450)
                .protein(20)
                .fats(15)
                .carbs(55)
                .assumptions(List.of("no-gluten"))
                .build();

        assertThat(dto).isEqualTo(dto2);
        assertThat(dto.hashCode()).isEqualTo(dto2.hashCode());

        
        dto2.setMenuItemName("Pepperoni");
        dto2.setCalories(600);
        assertThat(dto2.getMenuItemName()).isEqualTo("Pepperoni");
        assertThat(dto2.getCalories()).isEqualTo(600);
        assertThat(dto2).isNotEqualTo(dto);
    }
}