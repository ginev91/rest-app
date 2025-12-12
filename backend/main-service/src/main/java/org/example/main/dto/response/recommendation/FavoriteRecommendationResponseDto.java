package org.example.main.dto.response.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FavoriteRecommendationResponseDto implements Serializable {
    private UUID id;
    private List<String> ingredients;
    private String description;
    private String matchedMenuItemId;
    private UUID menuItemId;
    private String menuItemName;
    private Integer calories;
    private Integer protein;
    private Integer fats;
    private Integer carbs;
    private List<String> assumptions;
    private UUID createdBy;
    private OffsetDateTime createdAt;
}