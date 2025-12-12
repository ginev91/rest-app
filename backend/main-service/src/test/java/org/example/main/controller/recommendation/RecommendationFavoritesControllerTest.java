package org.example.main.controller.recommendation;

import org.example.main.dto.response.recommendation.FavoriteRecommendationResponseDto;
import org.example.main.model.recommendation.FavoriteRecommendation;
import org.example.main.repository.recommendation.FavoriteRecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationFavoritesControllerTest {

    @Mock
    FavoriteRecommendationRepository repository;

    @InjectMocks
    RecommendationFavoritesController controller;

    @Test
    void listFavorites_withIngredients_returnsDtoList() {
        FavoriteRecommendation ent = FavoriteRecommendation.builder()
                .id(UUID.randomUUID())
                .menuItemId(UUID.randomUUID())
                .menuItemName("Dish")
                .description("Tasty")
                .ingredients("tomato,cheese")
                .calories(200)
                .protein(10)
                .fats(5)
                .carbs(30)
                .createdBy(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build();

        when(repository.findAll()).thenReturn(List.of(ent));

        ResponseEntity<List<FavoriteRecommendationResponseDto>> resp = controller.listFavorites(null);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<FavoriteRecommendationResponseDto> body = resp.getBody();
        assertThat(body).hasSize(1);
        FavoriteRecommendationResponseDto dto = body.get(0);
        assertThat(dto.getMenuItemName()).isEqualTo("Dish");
        assertThat(dto.getIngredients()).containsExactly("tomato", "cheese");
        verify(repository).findAll();
    }

    @Test
    void listFavorites_withNullIngredients_producesNullIngredientsInDto() {
        FavoriteRecommendation ent = FavoriteRecommendation.builder()
                .id(UUID.randomUUID())
                .menuItemName("Plain")
                .ingredients(null)
                .createdAt(OffsetDateTime.now())
                .build();

        when(repository.findAll()).thenReturn(List.of(ent));

        ResponseEntity<List<FavoriteRecommendationResponseDto>> resp = controller.listFavorites(Boolean.TRUE);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<FavoriteRecommendationResponseDto> body = resp.getBody();
        assertThat(body).hasSize(1);
        FavoriteRecommendationResponseDto dto = body.get(0);
        assertThat(dto.getIngredients()).isNull();
        verify(repository).findAll();
    }

    @Test
    void addFavorite_withIngredients_savesAndReturnsDto() {
        FavoriteRecommendationResponseDto req = FavoriteRecommendationResponseDto.builder()
                .menuItemId(UUID.randomUUID())
                .menuItemName("New Dish")
                .description("Desc")
                .ingredients(List.of("a", "b"))
                .calories(100)
                .protein(5)
                .fats(3)
                .carbs(10)
                .build();

        // make repository.save assign an id (simulate JPA)
        when(repository.save(any(FavoriteRecommendation.class))).thenAnswer(inv -> {
            FavoriteRecommendation arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return arg;
        });

        ResponseEntity<FavoriteRecommendationResponseDto> resp = controller.addFavorite(req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        FavoriteRecommendationResponseDto out = resp.getBody();
        assertThat(out).isNotNull();
        assertThat(out.getMenuItemName()).isEqualTo("New Dish");
        assertThat(out.getIngredients()).containsExactly("a", "b");

        // verify repository.save received an entity whose ingredients string matches join
        ArgumentCaptor<FavoriteRecommendation> cap = ArgumentCaptor.forClass(FavoriteRecommendation.class);
        verify(repository).save(cap.capture());
        FavoriteRecommendation saved = cap.getValue();
        assertThat(saved.getIngredients()).isEqualTo("a,b");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void addFavorite_withNullIngredients_savesAndReturnsDtoWithNullIngredients() {
        FavoriteRecommendationResponseDto req = FavoriteRecommendationResponseDto.builder()
                .menuItemName("NoIng")
                .description("No ingredients")
                .ingredients(null)
                .build();

        when(repository.save(any(FavoriteRecommendation.class))).thenAnswer(inv -> {
            FavoriteRecommendation arg = inv.getArgument(0);
            arg.setId(UUID.randomUUID());
            return arg;
        });

        ResponseEntity<FavoriteRecommendationResponseDto> resp = controller.addFavorite(req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        FavoriteRecommendationResponseDto out = resp.getBody();
        assertThat(out).isNotNull();
        assertThat(out.getMenuItemName()).isEqualTo("NoIng");
        assertThat(out.getIngredients()).isNull();

        verify(repository).save(any(FavoriteRecommendation.class));
    }

    @Test
    void deleteFavorite_notFound_returns404() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        ResponseEntity<?> resp = controller.deleteFavorite(id);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        verify(repository).existsById(id);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteFavorite_found_deletesAndReturnsNoContent() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);
        doNothing().when(repository).deleteById(id);

        ResponseEntity<?> resp = controller.deleteFavorite(id);

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(repository).existsById(id);
        verify(repository).deleteById(id);
    }
}