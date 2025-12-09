package org.example.main.model;

import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.OrderItemStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderItemTest {

    @Test
    void getMenuItemId_and_setMenuItemId_behaviour() {
        OrderItem oi = new OrderItem();
        assertThat(oi.getMenuItemId()).isNull();

        UUID id = UUID.randomUUID();
        oi.setMenuItemId(id);

        assertThat(oi.getMenuItem()).isNotNull();
        assertThat(oi.getMenuItemId()).isEqualTo(id);
        assertThat(oi.getMenuItem().getId()).isEqualTo(id);
    }

    @Test
    void isKitchenItem_variants() {
        OrderItem oi = new OrderItem();

        // null menuItem => false
        oi.setMenuItem(null);
        assertThat(oi.isKitchenItem()).isFalse();

        // menuItem with KITCHEN => true
        MenuItem m1 = new MenuItem();
        m1.setItemType(ItemType.KITCHEN);
        oi.setMenuItem(m1);
        assertThat(oi.isKitchenItem()).isTrue();

        // menuItem with null itemType => true (service treats null as kitchen)
        MenuItem m2 = new MenuItem();
        m2.setItemType(null);
        oi.setMenuItem(m2);
        assertThat(oi.isKitchenItem()).isTrue();

        // menuItem with BAR => false
        MenuItem m3 = new MenuItem();
        m3.setItemType(ItemType.BAR);
        oi.setMenuItem(m3);
        assertThat(oi.isKitchenItem()).isFalse();
    }

    @Test
    void syncSnapshotFields_copiesFromMenuItem_and_appliesDefaults() throws Exception {
        OrderItem oi = new OrderItem();
        // prepare menu item with name and price
        MenuItem m = new MenuItem();
        m.setName("Test Dish");
        m.setPrice(BigDecimal.valueOf(4.25));
        oi.setMenuItem(m);

        // ensure snapshot fields are null to start
        oi.setMenuItemName(null);
        oi.setPrice(null);

        // invoke private lifecycle method
        Method sync = OrderItem.class.getDeclaredMethod("syncSnapshotFields");
        sync.setAccessible(true);
        sync.invoke(oi);

        assertThat(oi.getMenuItemName()).isEqualTo("Test Dish");
        assertThat(oi.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(4.25));

        // now test when menuItem has null name/price -> defaults applied
        OrderItem oi2 = new OrderItem();
        MenuItem mNull = new MenuItem();
        mNull.setName(null);
        mNull.setPrice(null);
        oi2.setMenuItem(mNull);
        oi2.setMenuItemName(null);
        oi2.setPrice(null);

        sync.invoke(oi2);

        assertThat(oi2.getMenuItemName()).isEqualTo("");
        assertThat(oi2.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void syncSnapshotFields_preservesExistingSnapshotValues() throws Exception {
        OrderItem oi = new OrderItem();
        MenuItem m = new MenuItem();
        m.setName("MenuName");
        m.setPrice(BigDecimal.valueOf(2.50));
        oi.setMenuItem(m);

        // set explicit snapshot values
        oi.setMenuItemName("Existing Snapshot");
        oi.setPrice(BigDecimal.valueOf(9.99));

        Method sync = OrderItem.class.getDeclaredMethod("syncSnapshotFields");
        sync.setAccessible(true);
        sync.invoke(oi);

        // existing snapshot must remain unchanged
        assertThat(oi.getMenuItemName()).isEqualTo("Existing Snapshot");
        assertThat(oi.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
    }

    @Test
    void setMenuItemId_createsMenuItem_when_missing() {
        OrderItem oi = new OrderItem();
        oi.setMenuItem(null);
        UUID id = UUID.randomUUID();
        oi.setMenuItemId(id);

        assertThat(oi.getMenuItem()).isNotNull();
        assertThat(oi.getMenuItemId()).isEqualTo(id);
    }

    @Test
    void getMenuItemId_returnsNull_when_menuItem_missing() {
        OrderItem oi = new OrderItem();
        oi.setMenuItem(null);
        assertThat(oi.getMenuItemId()).isNull();
    }

    @Test
    void default_values_for_status_and_quantity_are_not_assumed_here_but_methods_exist() {
        OrderItem oi = new OrderItem();
        oi.setQuantity(3);
        oi.setStatus(OrderItemStatus.PENDING);

        assertThat(oi.getQuantity()).isEqualTo(3);
        assertThat(oi.getStatus()).isEqualTo(OrderItemStatus.PENDING);
    }
}