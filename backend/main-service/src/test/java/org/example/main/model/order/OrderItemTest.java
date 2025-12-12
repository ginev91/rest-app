package org.example.main.model.order;

import org.example.main.model.menu.MenuItem;
import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.OrderItemStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderItemTest {

    
    private void callSync(OrderItem oi) throws Exception {
        Method sync = OrderItem.class.getDeclaredMethod("syncSnapshotFields");
        sync.setAccessible(true);
        sync.invoke(oi);
    }

    @Test
    void getMenuItemId_and_setMenuItemId_behaviour_when_menuItem_absent_and_present() {
        OrderItem oi = new OrderItem();

        
        oi.setMenuItem(null);
        assertThat(oi.getMenuItemId()).isNull();

        
        UUID id = UUID.randomUUID();
        oi.setMenuItemId(id);
        assertThat(oi.getMenuItem()).isNotNull();
        assertThat(oi.getMenuItemId()).isEqualTo(id);
        assertThat(oi.getMenuItem().getId()).isEqualTo(id);

        
        OrderItem oi2 = new OrderItem();
        MenuItem existing = new MenuItem();
        existing.setId(null);
        oi2.setMenuItem(existing);
        UUID id2 = UUID.randomUUID();
        oi2.setMenuItemId(id2);
        assertThat(oi2.getMenuItem()).isSameAs(existing);
        assertThat(oi2.getMenuItemId()).isEqualTo(id2);
    }

    @Test
    void isKitchenItem_all_variants() {
        OrderItem oi = new OrderItem();

        
        oi.setMenuItem(null);
        assertThat(oi.isKitchenItem()).isFalse();

        
        MenuItem m1 = new MenuItem();
        m1.setItemType(ItemType.KITCHEN);
        oi.setMenuItem(m1);
        assertThat(oi.isKitchenItem()).isTrue();

        
        MenuItem m2 = new MenuItem();
        m2.setItemType(null);
        oi.setMenuItem(m2);
        assertThat(oi.isKitchenItem()).isTrue();

        
        MenuItem m3 = new MenuItem();
        m3.setItemType(ItemType.BAR);
        oi.setMenuItem(m3);
        assertThat(oi.isKitchenItem()).isFalse();
    }

    @Test
    void syncSnapshotFields_copiesFromMenuItem_and_appliesDefaults_and_handles_blank_name() throws Exception {
        
        OrderItem oi = new OrderItem();
        MenuItem m = new MenuItem();
        m.setName("Test Dish");
        m.setPrice(BigDecimal.valueOf(4.25));
        oi.setMenuItem(m);
        oi.setMenuItemName(null);
        oi.setPrice(null);

        callSync(oi);

        assertThat(oi.getMenuItemName()).isEqualTo("Test Dish");
        assertThat(oi.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(4.25));

        
        OrderItem oiBlank = new OrderItem();
        MenuItem mBlank = new MenuItem();
        mBlank.setName("NonBlankName");
        mBlank.setPrice(BigDecimal.valueOf(1.50));
        oiBlank.setMenuItem(mBlank);
        
        oiBlank.setMenuItemName("   "); 
        oiBlank.setPrice(null);

        callSync(oiBlank);

        assertThat(oiBlank.getMenuItemName()).isEqualTo("NonBlankName");
        assertThat(oiBlank.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1.50));

        
        OrderItem oiNulls = new OrderItem();
        MenuItem mNull = new MenuItem();
        mNull.setName(null);
        mNull.setPrice(null);
        oiNulls.setMenuItem(mNull);
        oiNulls.setMenuItemName(null);
        oiNulls.setPrice(null);

        callSync(oiNulls);

        assertThat(oiNulls.getMenuItemName()).isEqualTo("");
        assertThat(oiNulls.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void syncSnapshotFields_preservesExistingSnapshotValues() throws Exception {
        OrderItem oi = new OrderItem();
        MenuItem m = new MenuItem();
        m.setName("MenuName");
        m.setPrice(BigDecimal.valueOf(2.50));
        oi.setMenuItem(m);

        
        oi.setMenuItemName("Existing Snapshot");
        oi.setPrice(BigDecimal.valueOf(9.99));

        callSync(oi);

        
        assertThat(oi.getMenuItemName()).isEqualTo("Existing Snapshot");
        assertThat(oi.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
    }

    @Test
    void syncSnapshotFields_when_menuItem_null_applies_defaults() throws Exception {
        
        OrderItem oi = new OrderItem();
        oi.setMenuItem(null);
        oi.setMenuItemName(null);
        oi.setPrice(null);

        callSync(oi);

        assertThat(oi.getMenuItem()).isNull();
        assertThat(oi.getMenuItemName()).isEqualTo("");
        assertThat(oi.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void basic_getters_and_setters_for_quantity_and_status() {
        OrderItem oi = new OrderItem();
        oi.setQuantity(3);
        oi.setStatus(OrderItemStatus.PENDING);

        assertThat(oi.getQuantity()).isEqualTo(3);
        assertThat(oi.getStatus()).isEqualTo(OrderItemStatus.PENDING);
    }

    @Test
    void getMenuItemId_returns_null_when_menuItem_present_but_id_not_set() {
        OrderItem oi = new OrderItem();
        MenuItem m = new MenuItem();
        m.setId(null);
        oi.setMenuItem(m);

        assertThat(oi.getMenuItemId()).isNull();
    }
}
