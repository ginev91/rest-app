package org.example.main.repository.category;

import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByItemType(ItemType itemType);
}