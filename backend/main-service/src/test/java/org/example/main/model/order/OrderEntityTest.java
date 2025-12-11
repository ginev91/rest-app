package org.example.main.model.order;

import org.example.main.model.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderEntityTest {

    @Test
    void prePersist_setsTimestamps_and_totalAmount_default() throws Exception {
        OrderEntity oe = new OrderEntity();
        oe.setId(UUID.randomUUID());
        oe.setStatus(OrderStatus.NEW);

        Method prePersist = OrderEntity.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(oe);

        assertThat(oe.getCreatedAt()).isNotNull();
        assertThat(oe.getUpdatedAt()).isNotNull();
        assertThat(oe.getTotalAmount()).isNotNull().isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void preUpdate_updatesUpdatedAt() throws Exception {
        OrderEntity oe = new OrderEntity();
        oe.setCreatedAt(OffsetDateTime.now().minusDays(1));
        oe.setUpdatedAt(oe.getCreatedAt());

        Method preUpdate = OrderEntity.class.getDeclaredMethod("preUpdate");
        preUpdate.setAccessible(true);
        preUpdate.invoke(oe);

        assertThat(oe.getUpdatedAt()).isNotNull();
        assertThat(oe.getUpdatedAt()).isAfterOrEqualTo(oe.getCreatedAt());
    }

    @Test
    void claimByWaiter_and_assign_unassign_behaviour() {
        OrderEntity oe = new OrderEntity();
        oe.setStatus(OrderStatus.NEW);
        UUID waiter = UUID.randomUUID();

        boolean claimed = oe.claimByWaiter(waiter);
        assertThat(claimed).isTrue();
        assertThat(oe.getWaiterId()).isEqualTo(waiter);
        assertThat(oe.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        boolean claimed2 = oe.claimByWaiter(UUID.randomUUID());
        assertThat(claimed2).isFalse();

        oe.unassignWaiter();
        assertThat(oe.getWaiterId()).isNull();

        oe.assignWaiter(waiter);
        assertThat(oe.getWaiterId()).isEqualTo(waiter);
    }
}