package org.example.main.mapper.kitchen;

import org.assertj.core.api.Assertions;
import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KitchenStatusMapperTest {

    @Test
    void toOrderItemStatus_variousValues() {
        Assertions.assertThat(KitchenStatusMapper.toOrderItemStatus(null)).isNull();
        assertThat(KitchenStatusMapper.toOrderItemStatus("  new  ")).isEqualTo(OrderItemStatus.PENDING);
        assertThat(KitchenStatusMapper.toOrderItemStatus("pending")).isEqualTo(OrderItemStatus.PENDING);
        assertThat(KitchenStatusMapper.toOrderItemStatus("preparing")).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(KitchenStatusMapper.toOrderItemStatus("IN_PROGRESS")).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(KitchenStatusMapper.toOrderItemStatus("inprogress")).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(KitchenStatusMapper.toOrderItemStatus("ready")).isEqualTo(OrderItemStatus.READY);
        assertThat(KitchenStatusMapper.toOrderItemStatus("served")).isEqualTo(OrderItemStatus.SERVED);
        assertThat(KitchenStatusMapper.toOrderItemStatus("completed")).isEqualTo(OrderItemStatus.SERVED);
        assertThat(KitchenStatusMapper.toOrderItemStatus("canceled")).isEqualTo(OrderItemStatus.CANCELLED);

        assertThat(KitchenStatusMapper.toOrderItemStatus("bogus-status")).isNull();
    }

    @Test
    void toOrderStatus_and_defaults_and_isTerminal() {
        assertThat(KitchenStatusMapper.toOrderStatus(null)).isNull();
        assertThat(KitchenStatusMapper.toOrderStatus("new")).isEqualTo(OrderStatus.NEW);
        assertThat(KitchenStatusMapper.toOrderStatus("preparing")).isEqualTo(OrderStatus.PROCESSING);
        assertThat(KitchenStatusMapper.toOrderStatus("IN_PROGRESS")).isEqualTo(OrderStatus.PROCESSING);
        assertThat(KitchenStatusMapper.toOrderStatus("ready")).isEqualTo(OrderStatus.READY);
        assertThat(KitchenStatusMapper.toOrderStatus("served")).isEqualTo(OrderStatus.COMPLETED);
        assertThat(KitchenStatusMapper.toOrderStatus("completed")).isEqualTo(OrderStatus.COMPLETED);
        assertThat(KitchenStatusMapper.toOrderStatus("canceled")).isEqualTo(OrderStatus.CANCELLED);

        assertThat(KitchenStatusMapper.toOrderItemStatusOrDefault("bogus")).isEqualTo(OrderItemStatus.PENDING);
        assertThat(KitchenStatusMapper.toOrderStatusOrDefault("bogus")).isEqualTo(OrderStatus.PROCESSING);

        assertThat(KitchenStatusMapper.isTerminal("completed")).isTrue();
        assertThat(KitchenStatusMapper.isTerminal("cancelled")).isTrue();
        assertThat(KitchenStatusMapper.isTerminal("ready")).isFalse();
    }
}