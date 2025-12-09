package org.example.main.mapper;

import org.example.main.dto.kitchen.KitchenInfoDto;
import org.example.main.feign.KitchenClient;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;


class KitchenResponseMapperTest {

    @Test
    void toKitchenInfo_returnsNullWhenInputNull() {
        assertThat(KitchenResponseMapper.toKitchenInfo(null)).isNull();
    }

    @Test
    void toKitchenInfo_mapsFieldsCorrectly() {
        KitchenClient.KitchenOrderResponse resp = new KitchenClient.KitchenOrderResponse();
        UUID id = UUID.randomUUID();
        try {
            var idField = resp.getClass().getField("id");
            idField.set(resp, id);
        } catch (NoSuchFieldException e) {
            try {
                var idField = resp.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(resp, id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            var statusField = resp.getClass().getField("status");
            statusField.set(resp, "READY");
        } catch (NoSuchFieldException e) {
            try {
                var statusField = resp.getClass().getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(resp, "READY");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        KitchenInfoDto dto = KitchenResponseMapper.toKitchenInfo(resp);
        assertThat(dto).isNotNull();
        assertThat(dto.getKitchenOrderId()).isEqualTo(id);
        assertThat(dto.getSourceOrderId()).isEqualTo(id);
        assertThat(dto.getStatus()).isEqualTo("READY");
    }
}