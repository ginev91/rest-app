package org.example.main.model.table;

import org.example.main.model.enums.TableStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RestaurantTableModelTest {

    @Test
    void defaults_and_setters() {
        RestaurantTable t = new RestaurantTable();

        assertThat(t.getCurrentOccupancy()).isEqualTo(0);
        assertThat(t.getStatus()).isEqualTo(TableStatus.AVAILABLE);

        t.setCode("X1");
        t.setSeats(6);
        t.setPinCode("0000");
        t.setTableNumber(7);

        assertThat(t.getCode()).isEqualTo("X1");
        assertThat(t.getSeats()).isEqualTo(6);
        assertThat(t.getPinCode()).isEqualTo("0000");
        assertThat(t.getTableNumber()).isEqualTo(7);
    }
}