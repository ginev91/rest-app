package org.example.main.service.category;

import org.example.main.model.category.CategoryEntity;

import java.util.List;
import java.util.UUID;

public interface ICategoryService {
    List<CategoryEntity> findAll();
    CategoryEntity findById(UUID id);
    CategoryEntity create(CategoryEntity entity);
    CategoryEntity update(UUID id, CategoryEntity changes);
    void delete(UUID id);
}