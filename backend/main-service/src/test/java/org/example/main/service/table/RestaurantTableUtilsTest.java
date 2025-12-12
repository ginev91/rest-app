package org.example.main.service.table;

import org.example.main.model.table.RestaurantTable;
import org.example.main.repository.table.RestaurantTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantTableUtilsTest {

    @Mock
    RestaurantTableRepository tableRepository;

    @Test
    void privateConstructor_covered() throws Exception {
        Constructor<RestaurantTableUtils> c = RestaurantTableUtils.class.getDeclaredConstructor();
        c.setAccessible(true);
        c.newInstance();
    }

    @Test
    void ensureTableNumber_returnsExistingAndDoesNotCallRepo() {
        RestaurantTable t = new RestaurantTable();
        t.setTableNumber(5);

        int out = RestaurantTableUtils.ensureTableNumber(t, tableRepository);

        assertThat(out).isEqualTo(5);
        verifyNoInteractions(tableRepository);
    }

    @Test
    void ensureTableNumber_assignsFirstFree() {
        RestaurantTable t = new RestaurantTable();

        when(tableRepository.findByTableNumber(1)).thenReturn(Optional.empty());

        int out = RestaurantTableUtils.ensureTableNumber(t, tableRepository);

        assertThat(out).isEqualTo(1);
        assertThat(t.getTableNumber()).isEqualTo(1);
        verify(tableRepository).findByTableNumber(1);
    }

    @Test
    void ensureTableNumber_skipsOccupiedNumbers_then_assigns() {
        RestaurantTable t = new RestaurantTable();

        when(tableRepository.findByTableNumber(anyInt())).thenAnswer(inv -> {
            int n = inv.getArgument(0);
            
            return n <= 3 ? Optional.of(new RestaurantTable()) : Optional.empty();
        });

        int out = RestaurantTableUtils.ensureTableNumber(t, tableRepository);

        assertThat(out).isEqualTo(4);
        assertThat(t.getTableNumber()).isEqualTo(4);
        verify(tableRepository, atLeastOnce()).findByTableNumber(anyInt());
    }

    @Test
    void ensureTableNumber_throwsWhenNoAvailable() {
        RestaurantTable t = new RestaurantTable();

        
        when(tableRepository.findByTableNumber(anyInt())).thenReturn(Optional.of(new RestaurantTable()));

        assertThatThrownBy(() -> RestaurantTableUtils.ensureTableNumber(t, tableRepository))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No available table numbers");
    }

    @Test
    void formatCode_and_pin_methods_and_sanitize_and_isValidPin() {
        
        assertThat(RestaurantTableUtils.formatCode(12)).isEqualTo("T12");

        
        String pin = RestaurantTableUtils.generatePinCode();
        assertThat(pin).matches("\\d{4}");

        
        String s1 = RestaurantTableUtils.sanitizePin(null);
        assertThat(s1).matches("\\d{4}");

        
        String s2 = RestaurantTableUtils.sanitizePin("abc");
        assertThat(s2).matches("\\d{4}");

        
        assertThat(RestaurantTableUtils.sanitizePin("123")).isEqualTo("0123");

        
        assertThat(RestaurantTableUtils.sanitizePin("123456")).isEqualTo("3456");

        
        assertThat(RestaurantTableUtils.sanitizePin("a12b")).isEqualTo("0012");

        
        assertThat(RestaurantTableUtils.isValidPin("1")).isTrue();
        assertThat(RestaurantTableUtils.isValidPin("1234")).isTrue();
        assertThat(RestaurantTableUtils.isValidPin("")).isFalse();
        assertThat(RestaurantTableUtils.isValidPin(null)).isFalse();
        assertThat(RestaurantTableUtils.isValidPin("12345")).isFalse();
        assertThat(RestaurantTableUtils.isValidPin("ab12")).isFalse();
    }
}