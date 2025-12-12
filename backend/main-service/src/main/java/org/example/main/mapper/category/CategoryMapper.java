package org.example.main.mapper.category;

import org.example.main.dto.request.category.CategoryRequestDto;
import org.example.main.dto.response.category.CategoryResponseDto;
import org.example.main.model.category.CategoryEntity;

public final class CategoryMapper {
    private CategoryMapper() {}

    public static CategoryResponseDto toResponse(CategoryEntity e) {
        if (e == null) return null;
        return CategoryResponseDto.builder()
                .id(e.getId())
                .itemType(e.getItemType())
                .build();
    }

    public static CategoryEntity toEntity(CategoryRequestDto dto) {
        if (dto == null) return null;
        CategoryEntity e = new CategoryEntity();
        e.setItemType(dto.getItemType());
        return e;
    }

    public static void copyToEntity(CategoryRequestDto dto, CategoryEntity entity) {
        if (dto == null || entity == null) return;
        entity.setItemType(dto.getItemType());
    }
}