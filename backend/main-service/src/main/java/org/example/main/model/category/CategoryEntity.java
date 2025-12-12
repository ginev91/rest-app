package org.example.main.model.category;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.example.main.model.enums.ItemType;

import java.util.UUID;

/**
 * CategoryEntity now represents an ItemType-backed category.
 * For now categories are the two ItemType values (KITCHEN, BAR).
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    // category is now represented by the ItemType enum and is unique
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, unique = true, length = 16)
    private ItemType itemType;
}