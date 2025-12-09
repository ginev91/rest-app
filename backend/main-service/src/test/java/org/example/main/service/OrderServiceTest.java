package org.example.main.service;

import feign.FeignException;
import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.request.OrderRequestDto.OrderItemRequest;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.feign.KitchenClient;
import org.example.main.mapper.KitchenStatusMapper;
import org.example.main.model.*;
import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.enums.OrderStatus;
import org.example.main.repository.MenuItemRepository;
import org.example.main.repository.OrderRepository;
import org.example.main.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    MenuItemRepository menuItemRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    KitchenClient kitchenClient;

    @Mock
    RestaurantTableService restaurantTableService;

    @InjectMocks
    OrderService orderService;

    private MenuItem mkMenuItem(UUID id, String name, ItemType type, BigDecimal price) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName(name);
        m.setItemType(type);
        m.setPrice(price);
        m.setCategory(new CategoryEntity());
        return m;
    }

    @Test
    void createOrder_nullOrEmpty_throwsBadRequest() {
        assertThatThrownBy(() -> orderService.createOrder(null))
                .isInstanceOf(ResponseStatusException.class);

        OrderRequestDto r = new OrderRequestDto();
        r.setItems(Collections.emptyList());
        assertThatThrownBy(() -> orderService.createOrder(r))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createOrder_menuItemNotFound_throwsBadRequest() {
        UUID mid = UUID.randomUUID();
        OrderRequestDto r = new OrderRequestDto();
        OrderItemRequest it = new OrderItemRequest();
        it.setMenuItemId(mid);
        it.setQuantity(1);
        r.setItems(List.of(it));

        when(menuItemRepository.findAllById(List.of(mid))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.createOrder(r))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Menu item not found");
    }

    @Test
    void createOrder_invalidQuantity_throwsBadRequest() {
        UUID mid = UUID.randomUUID();
        OrderRequestDto r = new OrderRequestDto();
        OrderItemRequest it = new OrderItemRequest();
        it.setMenuItemId(mid);
        it.setQuantity(0);
        r.setItems(List.of(it));

        MenuItem m = mkMenuItem(mid, "X", ItemType.KITCHEN, BigDecimal.valueOf(2.5));
        when(menuItemRepository.findAllById(List.of(mid))).thenReturn(List.of(m));

        assertThatThrownBy(() -> orderService.createOrder(r))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid quantity");
    }

    @Test
    void createOrder_createsNewOrder_and_notifiesKitchen_when_kitchen_items_present() {
        UUID midKitchen = UUID.randomUUID();
        UUID midBar = UUID.randomUUID();

        OrderRequestDto r = new OrderRequestDto();
        OrderItemRequest i1 = new OrderItemRequest();
        i1.setMenuItemId(midKitchen);
        i1.setQuantity(2);
        OrderItemRequest i2 = new OrderItemRequest();
        i2.setMenuItemId(midBar);
        i2.setQuantity(1);
        r.setItems(List.of(i1, i2));
        r.setTableNumber(10);
        r.setTableId(UUID.randomUUID());
        r.setCustomerId(UUID.randomUUID());

        MenuItem kitchenItem = mkMenuItem(midKitchen, "Stew", ItemType.KITCHEN, BigDecimal.valueOf(3.00));
        MenuItem barItem = mkMenuItem(midBar, "Cola", ItemType.BAR, BigDecimal.valueOf(1.50));

        when(menuItemRepository.findAllById(List.of(midKitchen, midBar))).thenReturn(List.of(kitchenItem, barItem));

        ArgumentCaptor<OrderEntity> saveCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            if (o.getItems() != null) {
                for (OrderItem it : o.getItems()) {
                    it.setMenuItemId(it.getMenuItem() != null ? it.getMenuItem().getId() : null);
                }
            }
            return o;
        });

        KitchenClient.KitchenOrderResponse kresp = new KitchenClient.KitchenOrderResponse();
        kresp.id = UUID.randomUUID();
        kresp.status = "CREATED";
        when(kitchenClient.createKitchenOrder(any())).thenReturn(kresp);

        OrderResponseDto resp = orderService.createOrder(r);

        assertThat(resp).isNotNull();
        assertThat(resp.getOrderId()).isNotNull();

        verify(orderRepository, atLeastOnce()).save(saveCaptor.capture());
        OrderEntity saved = saveCaptor.getValue();
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(7.5));

        verify(restaurantTableService).occupyTable(eq(r.getTableNumber()), anyInt());
        verify(kitchenClient).createKitchenOrder(any());
        verify(orderRepository, atLeast(2)).save(any(OrderEntity.class));
    }

    @Test
    void createOrder_mergesIntoExistingActiveOrder_and_notifiesKitchen_for_new_items() {
        UUID customer = UUID.randomUUID();
        UUID midKitchen = UUID.randomUUID();

        OrderRequestDto r = new OrderRequestDto();
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setMenuItemId(midKitchen);
        itemReq.setQuantity(1);
        r.setItems(List.of(itemReq));
        r.setCustomerId(customer);

        MenuItem kitchenItem = mkMenuItem(midKitchen, "Soup", ItemType.KITCHEN, BigDecimal.valueOf(4.0));
        when(menuItemRepository.findAllById(List.of(midKitchen))).thenReturn(List.of(kitchenItem));

        OrderEntity existing = new OrderEntity();
        existing.setId(UUID.randomUUID());
        existing.setCustomerId(customer);
        existing.setStatus(OrderStatus.NEW);
        existing.setItems(new ArrayList<>());
        existing.setTotalAmount(BigDecimal.valueOf(1.0));

        when(orderRepository.findWithItemsByCustomerUserId(customer)).thenReturn(List.of(existing));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        KitchenClient.KitchenOrderResponse kresp = new KitchenClient.KitchenOrderResponse();
        kresp.id = UUID.randomUUID();
        kresp.status = "CREATED";
        when(kitchenClient.createKitchenOrder(any())).thenReturn(kresp);

        OrderResponseDto out = orderService.createOrder(r);

        assertThat(out).isNotNull();
        assertThat(out.getOrderId()).isEqualTo(existing.getId());

        verify(orderRepository).findWithItemsByCustomerUserId(customer);
        verify(orderRepository, atLeastOnce()).save(existing);
        assertThat(existing.getItems()).hasSize(1);
        verify(kitchenClient).createKitchenOrder(any());
    }

    @Test
    void getOrderDetails_handlesKitchenClientFeignException_and_maps_entity() {
        UUID oid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);
        o.setItems(new ArrayList<>());
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));

        when(kitchenClient.getByOrder(oid)).thenThrow(mock(FeignException.class));

        OrderDetailsResponseDto dto = orderService.getOrderDetails(oid);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(oid.toString());
        assertThat(dto.getKitchenOrderId()).isNull();
    }

    @Test
    void updateKitchenStatus_updatesItemAndOrderStatuses() {
        UUID oid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.PROCESSING);
        OrderItem k1 = new OrderItem();
        k1.setMenuItem(new MenuItem());
        k1.getMenuItem().setItemType(ItemType.KITCHEN);
        k1.setStatus(OrderItemStatus.PENDING);
        k1.setMenuItemId(UUID.randomUUID());
        OrderItem nonKitchen = new OrderItem();
        nonKitchen.setMenuItem(new MenuItem());
        nonKitchen.getMenuItem().setItemType(ItemType.BAR);
        nonKitchen.setStatus(OrderItemStatus.PENDING);
        o.setItems(new ArrayList<>(List.of(k1, nonKitchen)));

        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String kitchenStatus = "READY";
        orderService.updateKitchenStatus(oid, kitchenStatus, UUID.randomUUID());

        OrderItemStatus expectedItemStatus = KitchenStatusMapper.toOrderItemStatus(kitchenStatus);
        if (expectedItemStatus != null) {
            assertThat(k1.getStatus()).isEqualTo(expectedItemStatus);
        } else {
            assertThat(k1.getStatus()).isEqualTo(OrderItemStatus.READY);
        }

        assertThat(nonKitchen.getStatus()).isEqualTo(OrderItemStatus.PENDING);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void getOrderSummary_found_and_notFound() {
        UUID oid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));

        OrderResponseDto dto = orderService.getOrderSummary(oid);
        assertThat(dto).isNotNull();
        assertThat(dto.getOrderId()).isEqualTo(oid);

        UUID missing = UUID.randomUUID();
        when(orderRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderSummary(missing))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrdersForUser_variousRoles_and_nullUser() {
        UUID uid = UUID.randomUUID();

        
        when(userRepository.findRoleByUserId(uid)).thenReturn("ROLE_ADMIN");
        OrderEntity e1 = new OrderEntity();
        e1.setId(UUID.randomUUID());
        when(orderRepository.findAll()).thenReturn(List.of(e1));

        List<OrderResponseDto> outAdmin = orderService.getOrdersForUser(uid);
        assertThat(outAdmin).hasSize(1);
        assertThat(outAdmin.get(0).getOrderId()).isEqualTo(e1.getId());

        
        when(userRepository.findRoleByUserId(uid)).thenReturn("ROLE_EMPLOYEE");
        OrderEntity e2 = new OrderEntity();
        e2.setId(UUID.randomUUID());
        when(orderRepository.findWithItemsByWaiterUserId(uid)).thenReturn(List.of(e2));

        List<OrderResponseDto> outEmp = orderService.getOrdersForUser(uid);
        assertThat(outEmp).hasSize(1);
        assertThat(outEmp.get(0).getOrderId()).isEqualTo(e2.getId());

        
        when(userRepository.findRoleByUserId(uid)).thenReturn(null);
        OrderEntity e3 = new OrderEntity();
        e3.setId(UUID.randomUUID());
        when(orderRepository.findWithItemsByCustomerUserId(uid)).thenReturn(List.of(e3));

        List<OrderResponseDto> outCust = orderService.getOrdersForUser(uid);
        assertThat(outCust).hasSize(1);
        assertThat(outCust.get(0).getOrderId()).isEqualTo(e3.getId());

        
        List<OrderResponseDto> outNull = orderService.getOrdersForUser(null);
        assertThat(outNull).isEmpty();
    }

    @Test
    void placeOrder_delegates_to_createOrder() {
        
        OrderService spySvc = spy(orderService);
        OrderResponseDto fake = OrderResponseDto.builder().orderId(UUID.randomUUID()).status("X").build();
        doReturn(fake).when(spySvc).createOrder(any());

        OrderRequestDto r = new OrderRequestDto();
        r.setItems(List.of(new OrderItemRequest(){{
            setMenuItemId(UUID.randomUUID());
            setQuantity(1);
        }}));

        UUID out = spySvc.placeOrder(r);
        assertThat(out).isEqualTo(fake.getOrderId());
    }

    @Test
    void updateStatus_updatesEntityAndSaves() {
        UUID oid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(oid, OrderStatus.CANCELLED);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(o);
    }

    @Test
    void getOrdersForTable_and_getAllOrders_and_activeOrder() {
        UUID tableId = UUID.randomUUID();
        OrderEntity o1 = new OrderEntity();
        o1.setId(UUID.randomUUID());
        o1.setTableId(tableId);
        o1.setStatus(OrderStatus.NEW);
        OrderEntity o2 = new OrderEntity();
        o2.setId(UUID.randomUUID());
        o2.setTableId(UUID.randomUUID());

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        List<OrderResponseDto> forTable = orderService.getOrdersForTable(tableId);
        assertThat(forTable).hasSize(1);
        assertThat(forTable.get(0).getOrderId()).isEqualTo(o1.getId());

        List<OrderResponseDto> all = orderService.getAllOrders();
        assertThat(all).hasSize(2);

        
        UUID userId = UUID.randomUUID();
        OrderEntity active = new OrderEntity();
        active.setId(UUID.randomUUID());
        active.setCustomerId(userId);
        active.setStatus(OrderStatus.NEW);
        when(orderRepository.findWithItemsByCustomerUserId(userId)).thenReturn(List.of(active));

        Optional<OrderResponseDto> act = orderService.getActiveOrderForUser(userId);
        assertThat(act).isPresent();
        assertThat(act.get().getOrderId()).isEqualTo(active.getId());

        Optional<OrderResponseDto> none = orderService.getActiveOrderForUser(UUID.randomUUID());
        assertThat(none).isEmpty();
    }

    @Test
    void cancelOrder_variants_noKitchen_and_withKitchen_success_and_failures() {
        UUID oid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        
        orderService.cancelOrder(oid);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        
        UUID kid = UUID.randomUUID();
        OrderEntity o2 = new OrderEntity();
        o2.setId(UUID.randomUUID());
        o2.setKitchenOrderId(kid);
        o2.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(o2.getId())).thenReturn(Optional.of(o2));
        doNothing().when(kitchenClient).cancelKitchenOrder(kid);

        orderService.cancelOrder(o2.getId());
        verify(kitchenClient).cancelKitchenOrder(kid);
        assertThat(o2.getKitchenStatus()).isEqualTo("CANCELLED");

        
        UUID kid2 = UUID.randomUUID();
        OrderEntity o3 = new OrderEntity();
        o3.setId(UUID.randomUUID());
        o3.setKitchenOrderId(kid2);
        o3.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(o3.getId())).thenReturn(Optional.of(o3));
        FeignException fe = mock(FeignException.class);
        when(fe.contentUTF8()).thenReturn("bad-body");
        when(fe.status()).thenReturn(500);
        doThrow(fe).when(kitchenClient).cancelKitchenOrder(kid2);

        orderService.cancelOrder(o3.getId());
        verify(kitchenClient).cancelKitchenOrder(kid2);
        assertThat(o3.getKitchenStatus()).isEqualTo("cancel_notify_failed");

        
        UUID kid3 = UUID.randomUUID();
        OrderEntity o4 = new OrderEntity();
        o4.setId(UUID.randomUUID());
        o4.setKitchenOrderId(kid3);
        o4.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(o4.getId())).thenReturn(Optional.of(o4));
        doThrow(new RuntimeException("boom")).when(kitchenClient).cancelKitchenOrder(kid3);

        orderService.cancelOrder(o4.getId());
        verify(kitchenClient).cancelKitchenOrder(kid3);
        assertThat(o4.getKitchenStatus()).isEqualTo("cancel_notify_failed");
    }

    @Test
    void getOrderDetails_withKitchenResponse_updatesStatuses() {
        UUID oid = UUID.randomUUID();
        OrderEntity o = new OrderEntity();
        o.setId(oid);
        o.setStatus(OrderStatus.NEW);
        OrderItem k1 = new OrderItem();
        k1.setMenuItem(new MenuItem());
        k1.getMenuItem().setItemType(ItemType.KITCHEN);
        k1.setStatus(OrderItemStatus.PENDING);
        k1.setMenuItemId(UUID.randomUUID());
        o.setItems(new ArrayList<>(List.of(k1)));
        UUID kitchenOrderId = UUID.randomUUID();
        o.setKitchenOrderId(kitchenOrderId);
        when(orderRepository.findById(oid)).thenReturn(Optional.of(o));

        
        KitchenClient.KitchenOrderResponse kr = new KitchenClient.KitchenOrderResponse();
        kr.id = kitchenOrderId;
        kr.status = "READY";
        when(kitchenClient.getByOrder(oid)).thenReturn(List.of(kr));

        OrderDetailsResponseDto dto = orderService.getOrderDetails(oid);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(oid.toString());
        
        assertThat(dto.getKitchenOrderId()).isEqualTo(kitchenOrderId.toString());
        
        assertThat(o.getKitchenStatus()).isEqualTo("READY");
        
        OrderItemStatus mapped = KitchenStatusMapper.toOrderItemStatus("READY");
        if (mapped != null) {
            assertThat(k1.getStatus()).isEqualTo(mapped);
        } else {
            assertThat(k1.getStatus()).isIn(OrderItemStatus.READY);
        }
    }
}