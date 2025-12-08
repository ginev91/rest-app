package org.example.kitchen.mapper;

import org.example.kitchen.dto.response.KitchenOrderResponse;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KitchenOrderMapperTest {

    @Test
    void toResponse_nullInput_returnsNull() {
        KitchenOrderResponse resp = KitchenOrderMapper.toResponse(null);
        assertNull(resp, "Mapping null should return null");
    }

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String itemsJson = "[{\"name\":\"Pizza\",\"qty\":2}]";
        KitchenOrderStatus status = KitchenOrderStatus.IN_PROGRESS;
        Instant created = Instant.now();
        Instant updated = created.plusSeconds(60);

        KitchenOrder entity = new KitchenOrder();
        entity.setId(id);
        entity.setOrderId(orderId);
        entity.setItemsJson(itemsJson);
        entity.setStatus(status);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);

        KitchenOrderResponse resp = KitchenOrderMapper.toResponse(entity);

        assertNotNull(resp, "Response should not be null for non-null input");
        assertEquals(id, resp.getId(), "id should be copied");
        assertEquals(orderId, resp.getOrderId(), "orderId should be copied");
        assertEquals(itemsJson, resp.getItemsJson(), "itemsJson should be copied");
        assertEquals(status, resp.getStatus(), "status should be copied");
        assertEquals(created, resp.getCreatedAt(), "createdAt should be copied");
        assertEquals(updated, resp.getUpdatedAt(), "updatedAt should be copied");
    }
}