package org.example.main.dto.response.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;


import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class MealRecommendationResponseDto implements Serializable {
    private List<String> ingredients;
    private String description;
    private String matchedMenuItemId;
    private String menuItemId;
    private String menuItemName;
    private Integer calories;
    private Integer protein;
    private Integer fats;
    private Integer carbs;
    private List<String> assumptions;

}