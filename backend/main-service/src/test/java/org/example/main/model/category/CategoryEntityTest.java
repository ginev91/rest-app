package org.example.main.model.category;

import org.example.main.model.enums.ItemType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CategoryEntityTest {

    @Test
    void lombok_accessors_and_builder_work_with_itemType() {
        CategoryEntity c = new CategoryEntity();
        c.setItemType(ItemType.KITCHEN);
        assertThat(c.getItemType()).isEqualTo(ItemType.KITCHEN);

        CategoryEntity built = CategoryEntity.builder()
                .itemType(ItemType.BAR)
                .build();
        assertThat(built.getItemType()).isEqualTo(ItemType.BAR);
    }
}