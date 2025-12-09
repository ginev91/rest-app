package org.example.main.scheduler;

import org.example.main.model.*;
import org.example.main.model.enums.OrderStatus;
import org.example.main.repository.MenuItemRepository;
import org.example.main.repository.OrderRepository;
import org.example.main.repository.RestaurantTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppScheduler that exercise the periodicJob and dailyReportJob paths.
 * These tests do not rely on Spring scheduling; they call methods directly and mock repositories.
 */
@ExtendWith(MockitoExtension.class)
class AppSchedulerTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    RestaurantTableRepository tableRepository;

    @Mock
    MenuItemRepository menuItemRepository;

    @Test
    void periodicJob_noActiveTables_logsAndReturns() {
        AppScheduler s = new AppScheduler(orderRepository, tableRepository, menuItemRepository);

        when(orderRepository.findByStatusIn(anyList())).thenReturn(Collections.emptyList());

        s.periodicJob();

        verify(orderRepository).findByStatusIn(anyList());
        verifyNoInteractions(tableRepository);
    }

    @Test
    void periodicJob_withActiveTables_fetchesTables_andLogsPerTable() {
        AppScheduler s = new AppScheduler(orderRepository, tableRepository, menuItemRepository);

        UUID tableId1 = UUID.randomUUID();
        UUID tableId2 = UUID.randomUUID();

        OrderEntity o1 = new OrderEntity();
        o1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o1.setStatus(OrderStatus.NEW);
        o1.setTableId(tableId1);

        OrderEntity o2 = new OrderEntity();
        o2.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        o2.setStatus(OrderStatus.NEW);
        o2.setTableId(tableId1);

        OrderEntity o3 = new OrderEntity();
        o3.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        o3.setStatus(OrderStatus.NEW);
        o3.setTableId(tableId2);

        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(o1, o2, o3));

        RestaurantTable t1 = new RestaurantTable();
        t1.setId(tableId1);
        t1.setCode("T1");
        RestaurantTable t2 = new RestaurantTable();
        t2.setId(tableId2);
        t2.setCode("T2");

        when(tableRepository.findAllById(anyList())).thenReturn(List.of(t1, t2));

        s.periodicJob();

        verify(orderRepository).findByStatusIn(anyList());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(tableRepository).findAllById(captor.capture());
        List<UUID> passed = captor.getValue();
        assertThat(passed).containsExactlyInAnyOrder(tableId1, tableId2);
    }

    @Test
    void dailyReportJob_emptyOrders_generatesZeroValues() {
        AppScheduler s = new AppScheduler(orderRepository, tableRepository, menuItemRepository);

        when(orderRepository.findWithItemsByCreatedAtBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());

        s.dailyReportJob();

        verify(orderRepository).findWithItemsByCreatedAtBetween(any(), any());
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void dailyReportJob_computesRevenue_and_topItems() {
        AppScheduler s = new AppScheduler(orderRepository, tableRepository, menuItemRepository);

        MenuItem m1 = new MenuItem();
        m1.setId(UUID.randomUUID());
        m1.setName("Cake");
        m1.setPrice(BigDecimal.valueOf(3.50));

        MenuItem m2 = new MenuItem();
        m2.setId(UUID.randomUUID());
        m2.setName("Tea");
        m2.setPrice(BigDecimal.valueOf(1.25));

        OrderItem oi1 = new OrderItem();
        oi1.setId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
        oi1.setMenuItem(m1);
        oi1.setQuantity(2);

        OrderItem oi2 = new OrderItem();
        oi2.setId(UUID.fromString("00000000-0000-0000-0000-000000000012"));
        oi2.setMenuItem(m2);
        oi2.setQuantity(3);

        OrderEntity order = new OrderEntity();
        order.setId(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        order.setItems(List.of(oi1, oi2));
        order.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findWithItemsByCreatedAtBetween(any(), any())).thenReturn(List.of(order));

        s.dailyReportJob();

        verify(orderRepository).findWithItemsByCreatedAtBetween(any(), any());
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void safeStream_and_itemTotal_handleNulls_andZeros() {
        AppScheduler s = new AppScheduler(orderRepository, tableRepository, menuItemRepository);

        OrderEntity orderWithNull = new OrderEntity();
        orderWithNull.setId(UUID.fromString("00000000-0000-0000-0000-000000000200"));
        orderWithNull.setItems(null);

        when(orderRepository.findWithItemsByCreatedAtBetween(any(), any())).thenReturn(List.of(orderWithNull));
        s.dailyReportJob();
        verify(orderRepository).findWithItemsByCreatedAtBetween(any(), any());

        OrderItem oi = new OrderItem();
        oi.setId(UUID.fromString("00000000-0000-0000-0000-000000000300"));
        oi.setMenuItem(null);
        oi.setQuantity(5);
        OrderEntity o2 = new OrderEntity();
        o2.setId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        o2.setItems(List.of(oi));
        when(orderRepository.findWithItemsByCreatedAtBetween(any(), any())).thenReturn(List.of(o2));
        s.dailyReportJob();
    }
}