package org.example.main.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.example.main.dto.kitchen.KitchenInfoDto;
import org.example.main.dto.request.order.OrderRequestDto;
import org.example.main.dto.request.order.OrderRequestDto.OrderItemRequest;
import org.example.main.dto.response.order.OrderDetailsResponseDto;
import org.example.main.dto.response.order.OrderItemResponseDto;
import org.example.main.dto.response.order.OrderResponseDto;
import org.example.main.feign.KitchenClient;
import org.example.main.mapper.kitchen.KitchenResponseMapper;
import org.example.main.mapper.kitchen.KitchenStatusMapper;
import org.example.main.model.menu.MenuItem;
import org.example.main.model.order.OrderEntity;
import org.example.main.model.order.OrderItem;
import org.example.main.model.role.Role;
import org.example.main.model.user.User;
import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.enums.OrderStatus;
import org.example.main.repository.menu.MenuItemRepository;
import org.example.main.repository.order.OrderItemRepository;
import org.example.main.repository.order.OrderRepository;
import org.example.main.repository.role.RoleRepository;
import org.example.main.repository.user.UserRepository;
import org.example.main.service.table.RestaurantTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.MockedStatic;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock MenuItemRepository menuItemRepository;
    @Mock UserRepository userRepository;
    @Mock KitchenClient kitchenClient;
    @Mock RestaurantTableService restaurantTableService;
    @Mock RoleRepository roleRepository;

    @InjectMocks OrderService orderService;

    private MenuItem menuKitchen;
    private MenuItem menuBar;
    private UUID menuKitchenId;
    private UUID menuBarId;

    @BeforeEach
    void setUp() {
        menuKitchenId = UUID.randomUUID();
        menuBarId = UUID.randomUUID();

        menuKitchen = new MenuItem();
        menuKitchen.setId(menuKitchenId);
        menuKitchen.setName("Steak");
        menuKitchen.setPrice(new BigDecimal("10.00"));
        menuKitchen.setItemType(ItemType.KITCHEN);

        menuBar = new MenuItem();
        menuBar.setId(menuBarId);
        menuBar.setName("Cola");
        menuBar.setPrice(new BigDecimal("2.50"));
        menuBar.setItemType(ItemType.BAR);
    }

    

    @Test
    void createOrder_nullRequest_throws() {
        assertThatThrownBy(() -> orderService.createOrder(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order must contain at least one item");
    }

    @Test
    void createOrder_missingMenuItem_throws() {
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest it = new OrderItemRequest();
        it.setMenuItemId(UUID.randomUUID());
        it.setQuantity(1);
        req.setItems(List.of(it));

        when(menuItemRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Menu item not found");
    }

    @Test
    void createOrder_invalidQuantity_throws() {
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest it = new OrderItemRequest();
        it.setMenuItemId(menuKitchenId);
        it.setQuantity(0);
        req.setItems(List.of(it));

        when(menuItemRepository.findAllById(anyList())).thenReturn(List.of(menuKitchen));

        assertThatThrownBy(() -> orderService.createOrder(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid quantity");
    }

    @Test
    void createOrder_existingActive_mergesAndNotifiesKitchen() {
        UUID customerId = UUID.randomUUID();

        OrderRequestDto req = new OrderRequestDto();
        req.setCustomerId(customerId);
        OrderItemRequest rIt = new OrderItemRequest();
        rIt.setMenuItemId(menuKitchenId);
        rIt.setQuantity(2);
        req.setItems(List.of(rIt));

        
        OrderEntity existing = new OrderEntity();
        existing.setId(UUID.randomUUID());
        existing.setCustomerId(customerId);
        existing.setStatus(OrderStatus.NEW);
        existing.setItems(new ArrayList<>());
        existing.setTotalAmount(new BigDecimal("5.00"));

        when(menuItemRepository.findAllById(List.of(menuKitchenId))).thenReturn(List.of(menuKitchen));
        when(orderRepository.findWithItemsByCustomerUserId(customerId)).thenReturn(List.of(existing));
        
        KitchenClient.KitchenOrderResponse kr = new KitchenClient.KitchenOrderResponse();
        kr.id = UUID.randomUUID();
        kr.status = "PENDING";
        when(kitchenClient.createKitchenOrder(any())).thenReturn(kr);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDto resp = orderService.createOrder(req);
        assertThat(resp).isNotNull();
        assertThat(resp.getOrderId()).isEqualTo(existing.getId());
        verify(orderRepository, atLeastOnce()).save(any());
    }

    @Test
    void createOrder_newOrder_createsAndNotifiesKitchen() {
        OrderRequestDto req = new OrderRequestDto();
        req.setTableNumber(5);
        OrderItemRequest r1 = new OrderItemRequest();
        r1.setMenuItemId(menuKitchenId);
        r1.setQuantity(1);
        req.setItems(List.of(r1));

        when(menuItemRepository.findAllById(List.of(menuKitchenId))).thenReturn(List.of(menuKitchen));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            
            if (e.getItems() != null) {
                for (OrderItem it : e.getItems()) {
                    it.setId(UUID.randomUUID());
                }
            }
            return e;
        });
        KitchenClient.KitchenOrderResponse resp = new KitchenClient.KitchenOrderResponse();
        resp.id = UUID.randomUUID();
        resp.status = "PENDING";
        when(kitchenClient.createKitchenOrder(any())).thenReturn(resp);

        OrderResponseDto r = orderService.createOrder(req);
        assertThat(r).isNotNull();
        assertThat(r.getOrderId()).isNotNull();
        verify(restaurantTableService).occupyTable(eq(5), anyInt());
    }

    @Test
    void createOrder_notifyKitchen_nullResponse_setsKitchenNotifyFailed() {
        
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest r1 = new OrderItemRequest();
        r1.setMenuItemId(menuKitchenId);
        r1.setQuantity(1);
        req.setItems(List.of(r1));

        when(menuItemRepository.findAllById(List.of(menuKitchenId))).thenReturn(List.of(menuKitchen));
        
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            if (e.getItems() != null) {
                for (OrderItem it : e.getItems()) {
                    if (it.getId() == null) it.setId(UUID.randomUUID());
                }
            }
            return e;
        });

        when(kitchenClient.createKitchenOrder(any())).thenReturn(null);

        OrderResponseDto resp = orderService.createOrder(req);
        assertThat(resp).isNotNull();
        
        ArgumentCaptor<OrderEntity> cap = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeast(1)).save(cap.capture());
        boolean foundFail = cap.getAllValues().stream().anyMatch(en -> "kitchen_notify_failed".equals(en.getKitchenStatus()));
        assertThat(foundFail).isTrue();
    }

    @Test
    void createOrder_notifyKitchen_throws_setsKitchenNotifyFailed() {
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest r1 = new OrderItemRequest();
        r1.setMenuItemId(menuKitchenId);
        r1.setQuantity(1);
        req.setItems(List.of(r1));

        when(menuItemRepository.findAllById(List.of(menuKitchenId))).thenReturn(List.of(menuKitchen));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            if (e.getItems() != null) {
                for (OrderItem it : e.getItems()) {
                    if (it.getId() == null) it.setId(UUID.randomUUID());
                }
            }
            return e;
        });

        when(kitchenClient.createKitchenOrder(any())).thenThrow(new RuntimeException("kitchen down"));

        OrderResponseDto resp = orderService.createOrder(req);
        assertThat(resp).isNotNull();
        ArgumentCaptor<OrderEntity> cap = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeast(1)).save(cap.capture());
        boolean foundFail = cap.getAllValues().stream().anyMatch(en -> "kitchen_notify_failed".equals(en.getKitchenStatus()));
        assertThat(foundFail).isTrue();
    }

    

    @Test
    void getOrderSummary_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderSummary(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrderDetails_kitchenClientThrows_returnsDetailsWithoutKitchen() {
        UUID id = UUID.randomUUID();
        OrderEntity e = new OrderEntity();
        e.setId(id);
        e.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(id)).thenReturn(Optional.of(e));
        when(kitchenClient.getByOrder(id)).thenThrow(new RuntimeException("feign fail"));

        OrderDetailsResponseDto dto = orderService.getOrderDetails(id);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id.toString());
    }

    @Test
    void getOrderDetails_withKitchenMapping_updatesItemStatuses() {
        UUID id = UUID.randomUUID();
        OrderEntity e = new OrderEntity();
        e.setId(id);
        e.setStatus(OrderStatus.NEW);
        OrderItem it = new OrderItem();
        it.setId(UUID.randomUUID());
        it.setMenuItem(new MenuItem()); 
        it.setStatus(OrderItemStatus.PENDING);
        e.setItems(List.of(it));

        when(orderRepository.findById(id)).thenReturn(Optional.of(e));

        KitchenClient.KitchenOrderResponse ko = new KitchenClient.KitchenOrderResponse();
        ko.id = UUID.randomUUID();
        ko.status = "COOKING";

        when(kitchenClient.getByOrder(id)).thenReturn(List.of(ko));

        try (MockedStatic<KitchenResponseMapper> krm = mockStatic(KitchenResponseMapper.class);
             MockedStatic<KitchenStatusMapper> ksm = mockStatic(KitchenStatusMapper.class)) {

            KitchenInfoDto ki = new KitchenInfoDto();
            ki.setKitchenOrderId(ko.id);
            ki.setStatus("COOKING");

            krm.when(() -> KitchenResponseMapper.toKitchenInfo(ko)).thenReturn(ki);
            ksm.when(() -> KitchenStatusMapper.toOrderItemStatus("COOKING")).thenReturn(OrderItemStatus.PREPARING);

            OrderDetailsResponseDto dto = orderService.getOrderDetails(id);

            assertThat(dto).isNotNull();
            assertThat(dto.getItems()).isNotEmpty();
            assertThat(e.getItems().get(0).getStatus()).isEqualTo(OrderItemStatus.PREPARING);
        }
    }

    

    @Test
    void getOrdersForUser_admin_returnsAll() {
        UUID userId = UUID.randomUUID();
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        when(roleRepository.findRolesByUserId(userId)).thenReturn(List.of(adminRole));
        OrderEntity e1 = new OrderEntity(); e1.setId(UUID.randomUUID());
        OrderEntity e2 = new OrderEntity(); e2.setId(UUID.randomUUID());
        when(orderRepository.findAll()).thenReturn(List.of(e1, e2));

        List<OrderResponseDto> res = orderService.getOrdersForUser(userId);
        assertThat(res).hasSize(2);
    }

    @Test
    void getOrdersForUser_employee_returnsWaiterOrders() {
        UUID userId = UUID.randomUUID();
        Role r = new Role(); r.setName("ROLE_EMPLOYEE");
        when(roleRepository.findRolesByUserId(userId)).thenReturn(List.of(r));
        OrderEntity e = new OrderEntity(); e.setId(UUID.randomUUID());
        when(orderRepository.findWithItemsByWaiterUserId(userId)).thenReturn(List.of(e));

        List<OrderResponseDto> res = orderService.getOrdersForUser(userId);
        assertThat(res).hasSize(1);
    }

    @Test
    void getOrdersForUser_customer_returnsCustomerOrders() {
        UUID userId = UUID.randomUUID();
        when(roleRepository.findRolesByUserId(userId)).thenReturn(Collections.emptyList());
        when(userRepository.findRoleByUserId(userId)).thenReturn(null);
        User user = new User();
        user.setRole(new Role());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        OrderEntity e = new OrderEntity(); e.setId(UUID.randomUUID());
        when(orderRepository.findWithItemsByCustomerUserId(userId)).thenReturn(List.of(e));

        List<OrderResponseDto> res = orderService.getOrdersForUser(userId);
        assertThat(res).hasSize(1);
    }

    @Test
    void getOrdersForUser_roleRepoThrows_legacyLookupUsed() {
        UUID userId = UUID.randomUUID();
        when(roleRepository.findRolesByUserId(userId)).thenThrow(new RuntimeException("db"));
        when(userRepository.findRoleByUserId(userId)).thenReturn("ROLE_EMPLOYEE");
        OrderEntity e = new OrderEntity(); e.setId(UUID.randomUUID());
        when(orderRepository.findWithItemsByWaiterUserId(userId)).thenReturn(List.of(e));

        List<OrderResponseDto> res = orderService.getOrdersForUser(userId);
        assertThat(res).hasSize(1);
    }


    @Test
    void placeOrder_callsCreateAndReturnsId() {
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest r = new OrderItemRequest();
        r.setMenuItemId(menuBarId);
        r.setQuantity(1);
        req.setItems(List.of(r));
        when(menuItemRepository.findAllById(List.of(menuBarId))).thenReturn(List.of(menuBar));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0); e.setId(UUID.randomUUID()); return e;
        });
        UUID id = orderService.placeOrder(req);
        assertThat(id).isNotNull();
    }

    @Test
    void cancelOrder_noKitchenId_updatesStatusOnly() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(id);

        verify(orderRepository, atLeastOnce()).save(any());
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_withKitchenId_feignFailure_setsCancelNotifyFailed() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(id);
        UUID kid = UUID.randomUUID();
        o.setKitchenOrderId(kid);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        doThrow(new RuntimeException("kitchen down")).when(kitchenClient).cancelKitchenOrder(kid);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(id);

        assertThat(o.getKitchenStatus()).isEqualTo("cancel_notify_failed");
    }

    

    @Test
    void updateOrderItemStatus_orderNotFound_throws() {
        UUID oid = UUID.randomUUID(), iid = UUID.randomUUID();
        when(orderRepository.findById(oid)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.updateOrderItemStatus(oid, iid, OrderItemStatus.PREPARING))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateOrderItemStatus_orderPaid_throws() {
        UUID oid = UUID.randomUUID(), iid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.updateOrderItemStatus(oid, iid, OrderItemStatus.PREPARING))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot update items");
    }

    @Test
    void updateOrderItemStatus_itemFoundInOrder_updates() {
        UUID oid = UUID.randomUUID(), iid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);
        OrderItem it = new OrderItem();
        it.setId(iid);
        it.setOrder(o);
        it.setStatus(OrderItemStatus.PENDING);
        o.setItems(new ArrayList<>(List.of(it)));
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateOrderItemStatus(oid, iid, OrderItemStatus.READY);
        assertThat(it.getStatus()).isEqualTo(OrderItemStatus.READY);
    }

    @Test
    void updateOrderItemStatus_itemNotInOrder_butInRepoAndBelongs_updates() {
        UUID oid = UUID.randomUUID(), iid = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(oid); o.setStatus(OrderStatus.NEW); o.setItems(new ArrayList<>());
        OrderItem repoItem = new OrderItem(); repoItem.setId(iid);
        OrderEntity parent = new OrderEntity(); parent.setId(oid);
        repoItem.setOrder(parent);

        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderItemRepository.findById(iid)).thenReturn(Optional.of(repoItem));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateOrderItemStatus(oid, iid, OrderItemStatus.PREPARING);
        assertThat(repoItem.getStatus()).isEqualTo(OrderItemStatus.PREPARING);
    }

    @Test
    void updateOrderItemStatus_repoItemBelongsToDifferentOrder_throwsBadRequest() {
        UUID oid = UUID.randomUUID(), iid = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(oid); o.setStatus(OrderStatus.NEW);
        OrderItem repoItem = new OrderItem(); repoItem.setId(iid);
        OrderEntity parent = new OrderEntity(); parent.setId(UUID.randomUUID()); 
        repoItem.setOrder(parent);

        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderItemRepository.findById(iid)).thenReturn(Optional.of(repoItem));

        assertThatThrownBy(() -> orderService.updateOrderItemStatus(oid, iid, OrderItemStatus.PREPARING))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void updateOrderItemStatus_itemNotFound_throwsWithAvailableIds() {
        UUID oid = UUID.randomUUID(), iid = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(oid); o.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderItemRepository.findById(iid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderItemStatus(oid, iid, OrderItemStatus.PREPARING))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order item not found");
    }

    

    @Test
    void updateOrderStatus_orderNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.updateOrderStatus(id, OrderStatus.NEW))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateOrderStatus_cannotUpdatePaid_throws() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(id); o.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        assertThatThrownBy(() -> orderService.updateOrderStatus(id, OrderStatus.NEW))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot update status");
    }

    @Test
    void updateOrderStatus_processing_requiresWaiter() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(id); o.setStatus(OrderStatus.NEW); o.setWaiterId(null);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        assertThatThrownBy(() -> orderService.updateOrderStatus(id, OrderStatus.PROCESSING))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("assigned to a waiter");
    }

    @Test
    void updateOrderStatus_completed_requiresAllItemsServed() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(id); o.setStatus(OrderStatus.PROCESSING);
        OrderItem it = new OrderItem(); it.setStatus(OrderItemStatus.PENDING);
        o.setItems(List.of(it));
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        assertThatThrownBy(() -> orderService.updateOrderStatus(id, OrderStatus.COMPLETED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot complete order");
    }

    @Test
    void updateOrderStatus_happyPath_updates() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(id); o.setStatus(OrderStatus.NEW); o.setWaiterId(UUID.randomUUID());
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateOrderStatus(id, OrderStatus.PROCESSING);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    

    @Test
    void parseOrderStatus_variousInputs() {
        assertThat(OrderService.parseOrderStatus("NEW")).isEqualTo(OrderStatus.NEW);
        assertThat(OrderService.parseOrderStatus("New")).isEqualTo(OrderStatus.NEW);
        assertThat(OrderService.parseOrderStatus("processing")).isEqualTo(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> OrderService.parseOrderStatus("unknown")).isInstanceOf(IllegalArgumentException.class);
    }

    

    @Test
    void claimOrder_assignsWhenUnassigned() {
        UUID id = UUID.randomUUID(), waiter = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean claimed = orderService.claimOrder(id, waiter);
        assertThat(claimed).isTrue();
        assertThat(o.getWaiterId()).isEqualTo(waiter);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void claimOrder_returnFalseWhenAlreadyAssigned() {
        UUID id = UUID.randomUUID(), waiter = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(id); o.setWaiterId(UUID.randomUUID());
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        boolean claimed = orderService.claimOrder(id, waiter);
        assertThat(claimed).isFalse();
    }

    

    @Test
    void updateKitchenStatus_updatesItemsAndOrderWhenAllKitchen_terminalMapping() {
        UUID orderId = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(orderId);
        OrderItem it = new OrderItem(); it.setMenuItem(new MenuItem()); it.setStatus(OrderItemStatus.PENDING);
        o.setItems(List.of(it));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<KitchenStatusMapper> ksm = mockStatic(KitchenStatusMapper.class)) {
            ksm.when(() -> KitchenStatusMapper.toOrderItemStatus("DONE")).thenReturn(OrderItemStatus.SERVED);
            ksm.when(() -> KitchenStatusMapper.isTerminal("DONE")).thenReturn(true);
            ksm.when(() -> KitchenStatusMapper.toOrderStatusOrDefault("DONE")).thenReturn(OrderStatus.COMPLETED);

            orderService.updateKitchenStatus(orderId, "DONE", UUID.randomUUID());

            assertThat(o.getKitchenStatus()).isEqualTo("DONE");
            assertThat(o.getItems().get(0).getStatus()).isEqualTo(OrderItemStatus.SERVED);
            assertThat(o.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }
    }

    @Test
    void updateKitchenStatus_nonKitchenDoesNotSetAllKitchen() {
        UUID orderId = UUID.randomUUID();
        OrderEntity o = new OrderEntity(); o.setId(orderId);
        MenuItem m = new MenuItem(); m.setItemType(ItemType.BAR);
        OrderItem it = new OrderItem(); it.setMenuItem(m); it.setStatus(OrderItemStatus.PENDING);
        o.setItems(List.of(it));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateKitchenStatus(orderId, "READY", UUID.randomUUID());
        
        assertThat(o.getStatus()).isNotEqualTo(OrderStatus.COMPLETED);
    }

    

    @Test
    void mapEntityToDetailsDto_and_mapToOrderResponseDto_coverNullsAndValues() {
        OrderEntity o = new OrderEntity();
        UUID id = UUID.randomUUID();
        o.setId(id);
        o.setCustomerId(UUID.randomUUID());
        o.setTableNumber(3);
        o.setStatus(OrderStatus.NEW);
        o.setTotalAmount(new BigDecimal("12.50"));
        o.setCreatedAt(OffsetDateTime.now());
        o.setUpdatedAt(OffsetDateTime.now());
        OrderItem it = new OrderItem();
        it.setId(UUID.randomUUID());
        it.setMenuItemId(UUID.randomUUID());
        it.setMenuItemName("X");
        it.setQuantity(2);
        it.setPrice(new BigDecimal("1.25"));
        o.setItems(List.of(it));

        when(userRepository.findById(o.getCustomerId())).thenReturn(Optional.of(new User()));
        when(orderRepository.findById(o.getId())).thenReturn(Optional.of(o));

        OrderDetailsResponseDto details = orderService.getOrderDetails(o.getId());
        assertThat(details.getId()).isEqualTo(o.getId().toString());
        assertThat(details.getItems()).isNotEmpty();

        OrderResponseDto resp = orderService.mapToOrderResponseDto(o);
        assertThat(resp.getOrderId()).isEqualTo(o.getId());
    }

    @Test
    void getOrdersForTable_null_returnsEmpty_and_getAllOrders_maps() {
        List<?> r = orderService.getOrdersForTable(null);
        assertThat(r).isEmpty();

        OrderEntity e = new OrderEntity();
        e.setId(UUID.randomUUID());
        when(orderRepository.findAll()).thenReturn(List.of(e));
        List<?> all = orderService.getAllOrders();
        assertThat(all).hasSize(1);
    }

    @Test
    void getActiveOrderForUser_filtersCompletedAndCancelled() {
        UUID userId = UUID.randomUUID();
        OrderEntity active = new OrderEntity(); active.setId(UUID.randomUUID()); active.setStatus(OrderStatus.NEW);
        OrderEntity completed = new OrderEntity(); completed.setId(UUID.randomUUID()); completed.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findWithItemsByCustomerUserId(userId)).thenReturn(List.of(completed, active));
        Optional<?> maybe = orderService.getActiveOrderForUser(userId);
        assertThat(maybe).isPresent();
    }

    @Test
    void updateOrderItemStatus_matchByMenuItemId_updates() {
        UUID oid = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);

        OrderItem it = new OrderItem();
        it.setMenuItemId(menuId);
        it.setStatus(OrderItemStatus.PENDING);
        it.setOrder(o);
        o.setItems(new ArrayList<>(List.of(it)));

        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        
        orderService.updateOrderItemStatus(oid, menuId, OrderItemStatus.PREPARING);
        assertThat(it.getStatus()).isEqualTo(OrderItemStatus.PREPARING);
    }

    @Test
    void createOrder_barOnly_skipsKitchenNotification() {
        
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest r1 = new OrderItemRequest();
        r1.setMenuItemId(menuBarId);
        r1.setQuantity(2);
        req.setItems(List.of(r1));

        when(menuItemRepository.findAllById(List.of(menuBarId))).thenReturn(List.of(menuBar));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            if (e.getItems() != null) {
                for (OrderItem it : e.getItems()) {
                    it.setId(UUID.randomUUID());
                }
            }
            return e;
        });

        OrderResponseDto resp = orderService.createOrder(req);
        assertThat(resp).isNotNull();
        verify(kitchenClient, never()).createKitchenOrder(any());
    }

    @Test
    void createOrder_notifyKitchen_serializationFails_setsKitchenNotifyFailed() throws Exception {
        OrderRequestDto req = new OrderRequestDto();
        OrderItemRequest r1 = new OrderItemRequest();
        r1.setMenuItemId(menuKitchenId);
        r1.setQuantity(1);
        req.setItems(List.of(r1));

        when(menuItemRepository.findAllById(List.of(menuKitchenId))).thenReturn(List.of(menuKitchen));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            if (e.getItems() != null) {
                for (OrderItem it : e.getItems()) {
                    if (it.getId() == null) it.setId(UUID.randomUUID());
                }
            }
            return e;
        });

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException(new com.fasterxml.jackson.core.JsonProcessingException("boom") {}));

        Field f = OrderService.class.getDeclaredField("objectMapper");
        f.setAccessible(true);
        f.set(orderService, mockMapper);

        OrderResponseDto resp = orderService.createOrder(req);
        assertThat(resp).isNotNull();

        ArgumentCaptor<OrderEntity> cap = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeast(1)).save(cap.capture());
        boolean foundFail = cap.getAllValues().stream().anyMatch(en -> "kitchen_notify_failed".equals(en.getKitchenStatus()));
        assertThat(foundFail).isTrue();
    }

    @Test
    void mapToOrderResponseDto_usesFullNameWhenPresent() {
        OrderEntity e = new OrderEntity();
        e.setId(UUID.randomUUID());
        UUID userId = UUID.randomUUID();
        e.setCustomerId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User() {{
            setFullName("John Doe");
            setUsername("jdoe");
        }}));

        OrderItem it = new OrderItem();
        it.setMenuItemId(UUID.randomUUID());
        e.setItems(List.of(it));
        OrderResponseDto dto = orderService.mapToOrderResponseDto(e);
        assertThat(dto.getUsername()).isEqualTo("John Doe");
    }

    @Test
    void updateStatus_setsStatusAndSaves() {
        UUID id = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(id);
        o.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(id)).thenReturn(Optional.of(o));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(id, OrderStatus.READY);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.READY);
    }
}