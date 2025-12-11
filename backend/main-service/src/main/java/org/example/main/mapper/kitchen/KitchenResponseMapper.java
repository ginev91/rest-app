package org.example.main.mapper.kitchen;

import org.example.main.feign.KitchenClient;
import org.example.main.dto.kitchen.KitchenInfoDto;

public final class KitchenResponseMapper {
    private KitchenResponseMapper() {}

    public static KitchenInfoDto toKitchenInfo(KitchenClient.KitchenOrderResponse resp) {
        if (resp == null) return null;
        KitchenInfoDto ki = new KitchenInfoDto();
        ki.setKitchenOrderId(resp.id);
        ki.setStatus(resp.status);
        ki.setSourceOrderId(resp.id);
        return ki;
    }
}