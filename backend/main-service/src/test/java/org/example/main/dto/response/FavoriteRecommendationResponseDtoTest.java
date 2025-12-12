package org.example.main.dto.response.recommendation;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FavoriteRecommendationResponseDto to exercise Lombok-generated methods.
 */
class FavoriteRecommendationResponseDtoTest {

    @Test
    void builder_getters_setters_equals_hashcode_toString() {
        UUID id = UUID.randomUUID();
        UUID menuItemId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        FavoriteRecommendationResponseDto dto = FavoriteRecommendationResponseDto.builder()
                .id(id)
                .ingredients(List.of("salt", "pepper"))
                .description("Favorite combo")
                .matchedMenuItemId("match-x")
                .menuItemId(menuItemId)
                .menuItemName("Special")
                .calories(320)
                .protein(12)
                .fats(8)
                .carbs(40)
                .assumptions(List.of("contains-nuts"))
                .createdBy(createdBy)
                .createdAt(now)
                .build();

        
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getIngredients()).containsExactly("salt", "pepper");
        assertThat(dto.getDescription()).isEqualTo("Favorite combo");
        assertThat(dto.getMatchedMenuItemId()).isEqualTo("match-x");
        assertThat(dto.getMenuItemId()).isEqualTo(menuItemId);
        assertThat(dto.getMenuItemName()).isEqualTo("Special");
        assertThat(dto.getCalories()).isEqualTo(320);
        assertThat(dto.getProtein()).isEqualTo(12);
        assertThat(dto.getFats()).isEqualTo(8);
        assertThat(dto.getCarbs()).isEqualTo(40);
        assertThat(dto.getAssumptions()).containsExactly("contains-nuts");
        assertThat(dto.getCreatedBy()).isEqualTo(createdBy);
        assertThat(dto.getCreatedAt()).isEqualTo(now);

        
        assertThat(dto.toString()).contains("Special", "Favorite combo");

        
        FavoriteRecommendationResponseDto dto2 = FavoriteRecommendationResponseDto.builder()
                .id(id)
                .ingredients(List.of("salt", "pepper"))
                .description("Favorite combo")
                .matchedMenuItemId("match-x")
                .menuItemId(menuItemId)
                .menuItemName("Special")
                .calories(320)
                .protein(12)
                .fats(8)
                .carbs(40)
                .assumptions(List.of("contains-nuts"))
                .createdBy(createdBy)
                .createdAt(now)
                .build();

        assertThat(dto).isEqualTo(dto2);
        assertThat(dto.hashCode()).isEqualTo(dto2.hashCode());

        
        dto2.setMenuItemName("Other");
        dto2.setCalories(999);
        assertThat(dto2.getMenuItemName()).isEqualTo("Other");
        assertThat(dto2.getCalories()).isEqualTo(999);
        assertThat(dto2).isNotEqualTo(dto);
    }
}