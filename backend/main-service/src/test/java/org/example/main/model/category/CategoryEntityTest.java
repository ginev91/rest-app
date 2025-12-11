package org.example.main.model.category;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CategoryEntityTest {

    @Test
    void gettersAndSetters_work_forCategoryEntity() {
        CategoryEntity c = new CategoryEntity();
        UUID id = UUID.randomUUID();
        c.setId(id);
        c.setName("Desserts");

        assertThat(c.getId()).isEqualTo(id);
        assertThat(c.getName()).isEqualTo("Desserts");
    }
}