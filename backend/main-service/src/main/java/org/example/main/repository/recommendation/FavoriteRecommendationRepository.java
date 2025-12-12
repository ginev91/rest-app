package org.example.main.repository.recommendation;

import org.example.main.model.recommendation.FavoriteRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteRecommendationRepository extends JpaRepository<FavoriteRecommendation, UUID> {
    List<FavoriteRecommendation> findByCreatedBy(UUID createdBy);
    List<FavoriteRecommendation> findByMenuItemId(UUID menuItemId);
}