package org.example.main.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MealRecommendationResponseDto implements Serializable {
    private String recipe;
    private String description;
    private String matchedMenuItemId;
    private String menuItemId;
    private String menuItemName;
    private Integer calories;
    private Integer protein;
    private Integer fat;
    private Integer carbs;
    private List<String> assumptions;
}