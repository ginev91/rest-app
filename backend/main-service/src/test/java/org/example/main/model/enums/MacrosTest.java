package org.example.main.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MacrosTest {

    @Test
    void gettersSetters_and_toString() {
        Macros m = new Macros();
        m.setProtein(10);
        m.setFat(5);
        m.setCarbs(20);

        assertThat(m.getProtein()).isEqualTo(10);
        assertThat(m.getFat()).isEqualTo(5);
        assertThat(m.getCarbs()).isEqualTo(20);
        assertThat(m.toString()).contains("protein", "fat", "carbs");
    }
}