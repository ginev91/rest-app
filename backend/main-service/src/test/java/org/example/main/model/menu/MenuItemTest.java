package org.example.main.model.menu;

import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.Macros;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MenuItemTest {

    @Test
    void builder_and_setters_work_with_category_itemType() {
        
        CategoryEntity cat = new CategoryEntity();
        cat.setItemType(ItemType.KITCHEN);

        
        MenuItem mi = MenuItem.builder()
                .name("Test Dish")
                .description("Tasty test")
                .price(BigDecimal.valueOf(9.99))
                .category(cat)
                .calories(250)
                .macros(new Macros(20, 10, 5))
                .available(true)
                .itemType(ItemType.KITCHEN)
                .build();

        
        assertThat(mi).isNotNull();
        assertThat(mi.getName()).isEqualTo("Test Dish");
        assertThat(mi.getCategory()).isNotNull();
        assertThat(mi.getCategory().getItemType()).isEqualTo(ItemType.KITCHEN);
        assertThat(mi.getItemType()).isEqualTo(ItemType.KITCHEN);
    }
}