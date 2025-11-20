package org.example.kitchen.mapper;

import org.example.kitchen.dto.response.KitchenOrderResponse;
import org.example.kitchen.model.KitchenOrder;

public final class KitchenOrderMapper {

    private KitchenOrderMapper() {}

    public static KitchenOrderResponse toResponse(KitchenOrder e) {
        if (e == null) return null;
        KitchenOrderResponse r = new KitchenOrderResponse();
        r.setId(e.getId());
        r.setOrderId(e.getOrderId());
        r.setItemsJson(e.getItemsJson());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}