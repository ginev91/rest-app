package org.example.main.model.menu;

import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.Macros;
import org.example.main.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class MenuItemTest {

    @Test
    void menuItem_fields_and_macros_and_category() {
        CategoryEntity cat = new CategoryEntity();
        UUID cid = UUID.randomUUID();
        cat.setId(cid);
        cat.setName("Drinks");

        Macros macros = new Macros(1, 2, 3);

        MenuItem mi = new MenuItem();
        UUID id = UUID.randomUUID();
        mi.setId(id);
        mi.setName("Latte");
        mi.setDescription("Nice");
        mi.setPrice(BigDecimal.valueOf(2.20));
        mi.setCategory(cat);
        mi.setCalories(150);
        mi.setMacros(macros);
        mi.setAvailable(true);
        mi.setItemType(ItemType.KITCHEN);

        assertThat(mi.getId()).isEqualTo(id);
        assertThat(mi.getName()).isEqualTo("Latte");
        assertThat(mi.getCategory()).isSameAs(cat);
        assertThat(mi.getMacros()).isSameAs(macros);
        assertThat(mi.isAvailable()).isTrue();
        assertThat(mi.getItemType()).isEqualTo(ItemType.KITCHEN);
    }
}