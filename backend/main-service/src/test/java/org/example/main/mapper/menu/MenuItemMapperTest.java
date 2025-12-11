package org.example.main.mapper.menu;

import org.example.main.dto.request.menu.MenuItemRequestDto;
import org.example.main.dto.response.menu.MenuItemResponseDto;
import org.example.main.model.category.CategoryEntity;
import org.example.main.model.menu.MenuItem;
import org.example.main.model.enums.Macros;
import org.example.main.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MenuItemMapper exercising all branches:
 * - null handling
 * - full mapping in toEntity / toResponse
 * - copyToEntity behavior for null/non-null fields and category creation
 */
class MenuItemMapperTest {

    @Test
    void toEntity_returnsNull_whenReqIsNull() {
        MenuItem mi = MenuItemMapper.toEntity(null);
        assertThat(mi).isNull();
    }

    @Test
    void toEntity_mapsAllFields_and_createsCategory_whenCategoryIdProvided() {
        UUID cid = UUID.randomUUID();
        MenuItemRequestDto req = new MenuItemRequestDto();
        req.setName("Test");
        req.setDescription("Desc");
        req.setPrice(BigDecimal.valueOf(9.99));
        req.setCalories(250);
        Macros macros = new Macros();
        macros.setProtein(10);
        req.setMacros(macros);
        req.setAvailable(true);
        req.setItemType(ItemType.KITCHEN);
        req.setCategoryId(cid);

        MenuItem mi = MenuItemMapper.toEntity(req);

        assertThat(mi).isNotNull();
        assertThat(mi.getName()).isEqualTo("Test");
        assertThat(mi.getDescription()).isEqualTo("Desc");
        assertThat(mi.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        assertThat(mi.getCalories()).isEqualTo(250);
        assertThat(mi.getMacros()).isSameAs(macros);
        assertThat(mi.isAvailable()).isTrue();
        assertThat(mi.getItemType()).isEqualTo(ItemType.KITCHEN);
        assertThat(mi.getCategory()).isNotNull();
        assertThat(mi.getCategory().getId()).isEqualTo(cid);
    }

    @Test
    void toResponse_returnsNull_whenEntityIsNull() {
        MenuItemResponseDto r = MenuItemMapper.toResponse(null);
        assertThat(r).isNull();
    }

    @Test
    void toResponse_mapsAllFields_and_handlesNullCategory() {
        MenuItem mi = new MenuItem();
        UUID id = UUID.randomUUID();
        mi.setId(id);
        mi.setName("NameX");
        mi.setDescription("DescX");
        mi.setPrice(BigDecimal.valueOf(5.5));
        mi.setCalories(100);
        mi.setAvailable(false);
        mi.setItemType(ItemType.BAR);
        Macros macros = new Macros();
        macros.setProtein(3);
        mi.setMacros(macros);

        
        MenuItemResponseDto r = MenuItemMapper.toResponse(mi);
        assertThat(r).isNotNull();
        assertThat(r.getId()).isEqualTo(id);
        assertThat(r.getName()).isEqualTo("NameX");
        assertThat(r.getDescription()).isEqualTo("DescX");
        assertThat(r.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(5.5));
        assertThat(r.getCalories()).isEqualTo(100);
        assertThat(r.isAvailable()).isFalse();
        assertThat(r.getItemType()).isEqualTo(ItemType.BAR);
        assertThat(r.getCategoryId()).isNull();
        assertThat(r.getMacros()).isSameAs(macros);

        
        CategoryEntity c = new CategoryEntity();
        UUID cid = UUID.randomUUID();
        c.setId(cid);
        mi.setCategory(c);

        MenuItemResponseDto r2 = MenuItemMapper.toResponse(mi);
        assertThat(r2.getCategoryId()).isEqualTo(cid);
    }

    @Test
    void copyToEntity_noopWhenReqOrTargetNull() {
        MenuItem target = new MenuItem();
        target.setName("orig");
        MenuItemRequestDto req = null;

        
        MenuItemMapper.copyToEntity(req, target);
        assertThat(target.getName()).isEqualTo("orig");

        
        MenuItemRequestDto someReq = new MenuItemRequestDto();
        someReq.setName("new");
        MenuItemMapper.copyToEntity(someReq, null);
        
    }

    @Test
    void copyToEntity_updatesOnlyNonNullFields_and_preservesExistingValues() {
        MenuItem target = new MenuItem();
        target.setName("keep-name");
        target.setDescription("keep-desc");
        target.setPrice(BigDecimal.valueOf(1.23));
        target.setCalories(10);
        target.setAvailable(false);
        target.setItemType(ItemType.BAR);
        CategoryEntity cat = new CategoryEntity();
        UUID existingCat = UUID.randomUUID();
        cat.setId(existingCat);
        target.setCategory(cat);

        MenuItemRequestDto req = new MenuItemRequestDto();
        
        req.setName(null);
        
        req.setDescription(null);
        
        req.setPrice(null);
        
        req.setCalories(42);
        
        Macros macros = new Macros();
        macros.setFats(5);
        req.setMacros(macros);
        
        req.setAvailable(true);
        
        req.setItemType(null);
        
        UUID newCat = UUID.randomUUID();
        req.setCategoryId(newCat);

        MenuItemMapper.copyToEntity(req, target);

        
        assertThat(target.getName()).isEqualTo("keep-name");
        
        assertThat(target.getDescription()).isNull();
        
        assertThat(target.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1.23));
        
        assertThat(target.getCalories()).isEqualTo(42);
        
        assertThat(target.getMacros()).isSameAs(macros);
        
        assertThat(target.isAvailable()).isTrue();
        
        assertThat(target.getItemType()).isEqualTo(ItemType.BAR);
        
        assertThat(target.getCategory()).isNotNull();
        assertThat(target.getCategory().getId()).isEqualTo(newCat);
    }

    @Test
    void copyToEntity_createsCategory_whenMissing_and_setsId() {
        MenuItem target = new MenuItem();
        target.setCategory(null);

        MenuItemRequestDto req = new MenuItemRequestDto();
        UUID cid = UUID.randomUUID();
        req.setCategoryId(cid);

        MenuItemMapper.copyToEntity(req, target);

        assertThat(target.getCategory()).isNotNull();
        assertThat(target.getCategory().getId()).isEqualTo(cid);
    }
}