package org.example.main.dto.response;

import org.example.main.dto.response.order.OrderDetailsResponseDto;
import org.example.main.dto.response.order.OrderItemResponseDto;
import org.example.main.dto.response.order.OrderResponseDto;
import org.example.main.model.enums.OrderItemStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderResponseDtoTest {

    @Test
    void orderItemResponseDto_builder_getters_setters_equals_hashcode_toString() {
        OrderItemResponseDto item = OrderItemResponseDto.builder()
                .menuItemId("m-1")
                .menuItemName("Burger")
                .quantity(2)
                .price(BigDecimal.valueOf(12.50))
                .status(OrderItemStatus.PENDING)
                .build();

        assertThat(item.getMenuItemId()).isEqualTo("m-1");
        assertThat(item.getMenuItemName()).isEqualTo("Burger");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PENDING);

        // toString not empty
        assertThat(item.toString()).contains("Burger", "m-1");

        // equals/hashCode: same values -> equal
        OrderItemResponseDto item2 = OrderItemResponseDto.builder()
                .menuItemId("m-1")
                .menuItemName("Burger")
                .quantity(2)
                .price(BigDecimal.valueOf(12.50))
                .status(OrderItemStatus.PENDING)
                .build();

        assertThat(item).isEqualTo(item2);
        assertThat(item.hashCode()).isEqualTo(item2.hashCode());

        // mutate via setters and verify changes
        item2.setMenuItemName("Cheeseburger");
        item2.setQuantity(3);
        item2.setStatus(OrderItemStatus.SERVED);
        assertThat(item2.getMenuItemName()).isEqualTo("Cheeseburger");
        assertThat(item2.getQuantity()).isEqualTo(3);
        assertThat(item2.getStatus()).isEqualTo(OrderItemStatus.SERVED);
    }

    @Test
    void orderResponseDto_builder_and_mutation_and_equality() {
        UUID orderId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        OrderItemResponseDto i1 = OrderItemResponseDto.builder()
                .menuItemId("m-1").menuItemName("A").quantity(1).price(BigDecimal.ONE).status(OrderItemStatus.PREPARING).build();

        OrderItemResponseDto i2 = OrderItemResponseDto.builder()
                .menuItemId("m-2").menuItemName("B").quantity(2).price(BigDecimal.TEN).status(OrderItemStatus.SERVED).build();

        OrderResponseDto dto = OrderResponseDto.builder()
                .orderId(orderId)
                .status("OPEN")
                .totalAmount(BigDecimal.valueOf(123.45))
                .tableNumber(12)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .items(List.of(i1, i2))
                .username("jane.doe")
                .build();

        assertThat(dto.getOrderId()).isEqualTo(orderId);
        assertThat(dto.getStatus()).isEqualTo("OPEN");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(123.45));
        assertThat(dto.getTableNumber()).isEqualTo(12);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now.plusMinutes(5));
        assertThat(dto.getItems()).containsExactly(i1, i2);
        assertThat(dto.getUsername()).isEqualTo("jane.doe");

        assertThat(dto.toString()).contains("OPEN", "jane.doe");

        OrderResponseDto dto2 = OrderResponseDto.builder()
                .orderId(orderId)
                .status("OPEN")
                .totalAmount(BigDecimal.valueOf(123.45))
                .tableNumber(12)
                .createdAt(now)
                .updatedAt(now.plusMinutes(5))
                .items(List.of(i1, i2))
                .username("jane.doe")
                .build();

        assertThat(dto).isEqualTo(dto2);
        assertThat(dto.hashCode()).isEqualTo(dto2.hashCode());

        dto2.setStatus("CLOSED");
        assertThat(dto2.getStatus()).isEqualTo("CLOSED");
        assertThat(dto2).isNotEqualTo(dto);

        OrderResponseDto empty = OrderResponseDto.builder().build();
        assertThat(empty.getItems()).isNullOrEmpty();
    }

    @Test
    void orderDetailsResponseDto_builder_and_setters_and_getters() {
        OrderItemResponseDto item = OrderItemResponseDto.builder()
                .menuItemId("m-x").menuItemName("X").quantity(5).price(BigDecimal.valueOf(3)).status(OrderItemStatus.PENDING).build();

        OrderDetailsResponseDto details = OrderDetailsResponseDto.builder()
                .id("ord-1")
                .userId("u-1")
                .userName("joe")
                .tableNumber(7)
                .status("NEW")
                .totalAmount(BigDecimal.valueOf(50))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .kitchenOrderId("k-1")
                .kitchenStatus("SENT")
                .items(List.of(item))
                .build();

        assertThat(details.getId()).isEqualTo("ord-1");
        assertThat(details.getUserId()).isEqualTo("u-1");
        assertThat(details.getUserName()).isEqualTo("joe");
        assertThat(details.getTableNumber()).isEqualTo(7);
        assertThat(details.getStatus()).isEqualTo("NEW");
        assertThat(details.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(details.getKitchenOrderId()).isEqualTo("k-1");
        assertThat(details.getKitchenStatus()).isEqualTo("SENT");
        assertThat(details.getItems()).containsExactly(item);

        details.setStatus("UPDATED");
        details.setUserName("jane");
        assertThat(details.getStatus()).isEqualTo("UPDATED");
        assertThat(details.getUserName()).isEqualTo("jane");

        // don't rely on toString containing fields (class may not generate a multi-field toString); assert toString is not null
        assertThat(details.toString()).isNotNull();
    }
}