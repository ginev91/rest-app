package org.example.main.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderItemStatusTest {

    @Test
    void getLabel_and_toString_returnLabel() {
        assertThat(OrderItemStatus.PENDING.getLabel()).isEqualTo("Pending");
        assertThat(OrderItemStatus.PENDING.toString()).isEqualTo("Pending");

        assertThat(OrderItemStatus.CANCELLED.getLabel()).isEqualTo("Cancelled");
        assertThat(OrderItemStatus.CANCELLED.toString()).isEqualTo("Cancelled");
    }

    @Test
    void fromLabel_acceptsDifferentCaseAndWhitespace_and_handles_multiword() {
        assertThat(OrderItemStatus.fromLabel(" pending ")).isEqualTo(OrderItemStatus.PENDING);
        assertThat(OrderItemStatus.fromLabel("PREPARING")).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(OrderItemStatus.fromLabel("in progress")).isEqualTo(OrderItemStatus.IN_PROGRESS);
        assertThat(OrderItemStatus.fromLabel("  Ready  ")).isEqualTo(OrderItemStatus.READY);
        assertThat(OrderItemStatus.fromLabel("SeRvEd")).isEqualTo(OrderItemStatus.SERVED);
    }

    @Test
    void fromLabel_null_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> OrderItemStatus.fromLabel(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label is null");
    }

    @Test
    void fromLabel_unknown_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> OrderItemStatus.fromLabel("not-a-status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown OrderStatus");
    }
}