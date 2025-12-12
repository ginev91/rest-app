package org.example.main.model.recommendation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorite_recommendations", indexes = {
        @Index(name = "idx_favrec_menu_item_id", columnList = "menu_item_id"),
        @Index(name = "idx_favrec_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteRecommendation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "menu_item_id")
    private UUID menuItemId;

    @Column(name = "menu_item_name")
    private String menuItemName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String ingredients;

    private Integer calories;
    private Integer protein;
    private Integer fats;
    private Integer carbs;

    @Column(name = "created_by", columnDefinition = "uuid", nullable = true)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
    }
}