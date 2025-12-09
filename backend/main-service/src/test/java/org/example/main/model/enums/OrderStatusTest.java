package org.example.main.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderStatusTest {

    @Test
    void getLabel_and_toString_returnLabel() {
        assertThat(OrderStatus.NEW.getLabel()).isEqualTo("New");
        assertThat(OrderStatus.NEW.toString()).isEqualTo("New");

        assertThat(OrderStatus.COMPLETED.getLabel()).isEqualTo("Completed");
        assertThat(OrderStatus.COMPLETED.toString()).isEqualTo("Completed");
    }

    @Test
    void fromLabel_acceptsDifferentCaseAndWhitespace() {
        assertThat(OrderStatus.fromLabel(" new ")).isEqualTo(OrderStatus.NEW);
        assertThat(OrderStatus.fromLabel("processing")).isEqualTo(OrderStatus.PROCESSING);
        assertThat(OrderStatus.fromLabel("READY")).isEqualTo(OrderStatus.READY);
        assertThat(OrderStatus.fromLabel("  Completed  ")).isEqualTo(OrderStatus.COMPLETED);
        assertThat(OrderStatus.fromLabel("paid")).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void fromLabel_null_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> OrderStatus.fromLabel(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label is null");
    }

    @Test
    void fromLabel_unknown_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> OrderStatus.fromLabel("not-a-status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown OrderStatus");
    }

    @Test
    void isActive_returnsTrueForNonTerminalStatuses_andFalseForTerminalOnes() {
        // active statuses
        assertThat(OrderStatus.NEW.isActive()).isTrue();
        assertThat(OrderStatus.PROCESSING.isActive()).isTrue();
        assertThat(OrderStatus.READY.isActive()).isTrue();

        // terminal statuses
        assertThat(OrderStatus.COMPLETED.isActive()).isFalse();
        assertThat(OrderStatus.PAID.isActive()).isFalse();
        assertThat(OrderStatus.CANCELLED.isActive()).isFalse();
    }
}