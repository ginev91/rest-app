package org.example.kitchen.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KitchenOrderStatusTest {

    @Test
    void fromString_null_returnsNull() {
        assertNull(KitchenOrderStatus.fromString(null));
    }

    @Test
    void fromString_exactMatch_returnsEnum() {
        assertEquals(KitchenOrderStatus.NEW, KitchenOrderStatus.fromString("NEW"));
        assertEquals(KitchenOrderStatus.COMPLETED, KitchenOrderStatus.fromString("COMPLETED"));
    }

    @Test
    void fromString_caseInsensitive_returnsEnum() {
        assertEquals(KitchenOrderStatus.PREPARING, KitchenOrderStatus.fromString("preparing"));
        assertEquals(KitchenOrderStatus.IN_PROGRESS, KitchenOrderStatus.fromString("In_PrOgReSs"));
    }

    @Test
    void fromString_withWhitespace_returnsEnum() {
        assertEquals(KitchenOrderStatus.READY, KitchenOrderStatus.fromString("  ready  "));
    }

    @Test
    void fromString_invalidValue_returnsNull() {
        assertNull(KitchenOrderStatus.fromString("not-a-status"));
        assertNull(KitchenOrderStatus.fromString("")); 
        assertNull(KitchenOrderStatus.fromString("  ")); 
    }

    @Test
    void allEnumValues_parseBackFromName() {
        for (KitchenOrderStatus s : KitchenOrderStatus.values()) {
            assertEquals(s, KitchenOrderStatus.fromString(s.name()));
            
            assertEquals(s, KitchenOrderStatus.fromString(s.name().toLowerCase()));
        }
    }
}