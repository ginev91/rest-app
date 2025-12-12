package org.example.main.mapper.category;

import org.example.main.dto.request.category.CategoryRequestDto;
import org.example.main.dto.response.category.CategoryResponseDto;
import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CategoryMapperTest {

    @Test
    void toResponse_returnsNull_whenEntityIsNull() {
        assertThat(CategoryMapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_mapsFields() {
        CategoryEntity e = new CategoryEntity();
        UUID id = UUID.randomUUID();
        e.setId(id);
        e.setItemType(ItemType.BAR);

        CategoryResponseDto dto = CategoryMapper.toResponse(e);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getItemType()).isEqualTo(ItemType.BAR);
    }

    @Test
    void toEntity_returnsNull_whenDtoIsNull() {
        assertThat(CategoryMapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_mapsFields() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setItemType(ItemType.KITCHEN);

        CategoryEntity e = CategoryMapper.toEntity(req);
        assertThat(e).isNotNull();
        assertThat(e.getItemType()).isEqualTo(ItemType.KITCHEN);
    }

    @Test
    void copyToEntity_noop_whenDtoOrEntityNull() {
        CategoryEntity entity = new CategoryEntity();
        entity.setItemType(ItemType.KITCHEN);

        // null dto -> no change
        CategoryMapper.copyToEntity(null, entity);
        assertThat(entity.getItemType()).isEqualTo(ItemType.KITCHEN);

        CategoryRequestDto dto = new CategoryRequestDto();
        dto.setItemType(ItemType.BAR);

        CategoryMapper.copyToEntity(dto, null);
    }

    @Test
    void copyToEntity_updatesEntityFields() {
        CategoryEntity entity = new CategoryEntity();
        entity.setItemType(ItemType.KITCHEN);

        CategoryRequestDto dto = new CategoryRequestDto();
        dto.setItemType(ItemType.BAR);

        CategoryMapper.copyToEntity(dto, entity);
        assertThat(entity.getItemType()).isEqualTo(ItemType.BAR);
    }
}