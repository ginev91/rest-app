package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.request.OrderRequestDto.OrderItemRequest;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderItemResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.feign.KitchenClient;
import org.example.main.model.MenuItem;
import org.example.main.model.OrderEntity;
import org.example.main.model.OrderItem;
import org.example.main.model.User;
import org.example.main.model.enums.ItemType;
import org.example.main.repository.MenuItemRepository;
import org.example.main.repository.OrderRepository;
import org.example.main.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@Slf4j
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final KitchenClient kitchenClient;
    private final RestaurantTableService restaurantTableService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        UserRepository userRepository,
                        KitchenClient kitchenClient,
                        RestaurantTableService restaurantTableService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.kitchenClient = kitchenClient;
        this.restaurantTableService = restaurantTableService;
    }

    /**
     * Create order and return lightweight response (OrderResponseDto).
     */
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        List<UUID> menuIds = request.getItems().stream()
                .map(OrderItemRequest::getMenuItemId)
                .collect(Collectors.toList());

        List<MenuItem> menuItems = menuItemRepository.findAllById(menuIds);
        Map<UUID, MenuItem> menuMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, m -> m));

        for (UUID id : menuIds) {
            if (!menuMap.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item not found: " + id);
            }
        }

        OrderEntity order = new OrderEntity();
        order.setTableId(request.getTableId());
        order.setTableNumber(request.getTableNumber());
        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(findUserName(request.getCustomerId()));
        order.setStatus("preparing");
        order.setCreatedAt(OffsetDateTime.now());

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid quantity for menu item: " + itemReq.getMenuItemId());
            }

            MenuItem mi = menuMap.get(itemReq.getMenuItemId());
            OrderItem oi = new OrderItem();
            oi.setMenuItem(mi);
            oi.setQuantity(itemReq.getQuantity());
            oi.setMenuItemName(mi.getName());
            oi.setPrice(mi.getPrice() != null ? mi.getPrice() : BigDecimal.ZERO);
            oi.setOrder(order);
            items.add(oi);

            BigDecimal line = oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity()));
            total = total.add(line);
        }

        order.setItems(items);
        order.setTotalAmount(total);

        OrderEntity saved = orderRepository.save(order);
        log.info("Order created: {} total={} by customer={}", saved.getId(), saved.getTotalAmount(), saved.getCustomerId());

        try {
            if (saved.getTableNumber() != null) {
                restaurantTableService.occupyTable(saved.getTableNumber(), 60); // occupy for 60 minutes
            }
        } catch (Exception ex) {
            log.warn("Failed to mark table {} occupied: {}", saved.getTableNumber(), ex.getMessage());
        }

        // Partition items by ItemType and notify kitchen only for KITCHEN items
        try {
            List<OrderItem> allSavedItems = saved.getItems() == null ? Collections.emptyList() : saved.getItems();

            List<OrderItem> kitchenItems = allSavedItems.stream()
                    .filter(it -> {
                        MenuItem mi = it.getMenuItem();
                        if (mi == null) return false;
                        ItemType type = mi.getItemType();
                        return type == ItemType.KITCHEN || type == null;
                    })
                    .collect(Collectors.toList());

            if (!kitchenItems.isEmpty()) {
                notifyKitchen(saved, kitchenItems);
            } else {
                log.debug("No kitchen items for order {}, skipping kitchen notification.", saved.getId());
            }
        } catch (Exception ex) {
            // log full stacktrace to help debugging
            log.error("Failed to notify kitchen service for order {}: ", saved.getId(), ex);
            try {
                saved.setKitchenStatus("kitchen_notify_failed");
                orderRepository.save(saved);
            } catch (Exception e) {
                log.warn("Failed to persist kitchen failure status for order {}: {}", saved.getId(), e.getMessage());
            }
        }

        return OrderResponseDto.builder()
                .orderId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderSummary(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return OrderResponseDto.builder()
                .orderId(o.getId())
                .status(o.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailsResponseDto getOrderDetails(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return mapEntityToDetailsDto(o);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersForUser(UUID userId) {
        if (userId == null) return Collections.emptyList();
        String userRole = userRepository.findRoleByUserId(userId);
        if (userRole != null && "ROLE_ADMIN".equals(userRole)) {
            List<OrderEntity> allOrders = orderRepository.findAll();
            return allOrders.stream()
                    .map(this::mapToOrderResponseDto)
                    .collect(Collectors.toList());
        } else if (userRole != null && "ROLE_EMPLOYEE".equals(userRole)) {
            List<OrderEntity> entities = orderRepository.findWithItemsByWaiterUserId(userId);
            return entities.stream()
                    .map(this::mapToOrderResponseDto)
                    .collect(Collectors.toList());
        }
        List<OrderEntity> entities = orderRepository.findWithItemsByCustomerUserId(userId);
        return entities.stream()
                .map(this::mapToOrderResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UUID placeOrder(OrderRequestDto dto) {
        OrderResponseDto resp = createOrder(dto);
        return resp.getOrderId();
    }

    @Override
    @Transactional
    public void callWaiter(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        o.setStatus("call_waiter");
        orderRepository.save(o);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        o.setStatus("cancelled");
        orderRepository.save(o);
    }

    @Override
    @Transactional
    public void updateStatus(UUID orderId, String status) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        o.setStatus(status);
        orderRepository.save(o);
    }

    @Override
    public List<OrderResponseDto> getOrdersForTable(UUID tableId) {
        if (tableId == null) return Collections.emptyList();

        List<OrderEntity> entities = orderRepository.findAll();
        log.info("Found {} orders for table {}", entities.size(), tableId);
        return entities.stream()
                .filter(e -> tableId.equals(e.getTableId()))
                .map(this::mapToOrderResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {
        List<OrderEntity> entities = orderRepository.findAll();
        return entities.stream()
                .map(this::mapToOrderResponseDto)
                .collect(Collectors.toList());
    }

    private String findUserName(UUID userId) {
        if (userId == null) return null;
        Optional<User> u = userRepository.findById(userId);
        return u.map(User::getFullName).orElse(null);
    }

    private OrderDetailsResponseDto mapEntityToDetailsDto(OrderEntity o) {
        List<OrderItemResponseDto> items = o.getItems().stream()
                .map(it -> OrderItemResponseDto.builder()
                        .menuItemId(it.getMenuItemId() != null ? it.getMenuItemId().toString() : null)
                        .menuItemName(it.getMenuItemName())
                        .quantity(it.getQuantity())
                        .price(it.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderDetailsResponseDto.builder()
                .id(o.getId() != null ? o.getId().toString() : null)
                .userId(o.getCustomerId() != null ? o.getCustomerId().toString() : null)
                .userName(o.getCustomerName())
                .tableNumber(o.getTableNumber())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .kitchenOrderId(o.getKitchenOrderId() != null ? o.getKitchenOrderId().toString() : null)
                .kitchenStatus(o.getKitchenStatus())
                .items(items)
                .build();
    }

    private OrderResponseDto mapToOrderResponseDto(OrderEntity e) {
        List<OrderItemResponseDto> items = (e.getItems() == null) ? Collections.emptyList() :
                e.getItems().stream()
                        .map(it -> OrderItemResponseDto.builder()
                                .menuItemId(it.getMenuItemId() != null ? it.getMenuItemId().toString() : null)
                                .menuItemName(it.getMenuItemName())
                                .quantity(it.getQuantity())
                                .price(it.getPrice())
                                .build())
                        .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(e.getId())
                .status(e.getStatus())
                .totalAmount(e.getTotalAmount())
                .tableNumber(e.getTableNumber())
                .createdAt(e.getCreatedAt())
                .items(items)
                .build();
    }

    @Transactional
    public void updateKitchenStatus(UUID orderId, String kitchenStatus, UUID kitchenOrderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setKitchenStatus(kitchenStatus);
        if (kitchenOrderId != null) {
            order.setKitchenOrderId(kitchenOrderId);
        }
        order.setUpdatedAt(OffsetDateTime.now());

// not proper handling order    set only kitcvhen status
//        if ("READY".equalsIgnoreCase(kitchenStatus)) {
//            boolean allKitchen = true;
//            if (order.getItems() == null || order.getItems().isEmpty()) {
//                allKitchen = false;
//            } else {
//                for (OrderItem item : order.getItems()) {
//
//                    boolean kitchenItem;
//                    try {
//                        kitchenItem = item.isKitchenItem();
//                    } catch (NoSuchMethodError | AbstractMethodError ex) {
//                        kitchenItem = false;
//                    }
//                    if (!kitchenItem) {
//                        allKitchen = false;
//                        break;
//                    }
//                }
//            }
//
//            if (allKitchen) {
//                log.info("All items for order {} are kitchen items — setting overall order status to READY", orderId);
//                order.setStatus("ready");
//            } else {
//                log.debug("Order {} contains non-kitchen items, not changing overall order status", orderId);
//            }
//        }
        orderRepository.save(order);
        log.info("Order {} kitchenStatus updated to {} (kitchenOrderId={})", orderId, kitchenStatus, kitchenOrderId);
    }

    /**
     * Build payload compatible with kitchen CreateKitchenOrderRequest but using the existing nested KitchenClient.KitchenOrderRequest.
     * We serialize items into a JSON string and set orderId -> itemsJson so kitchen validation passes.
     */
    private void notifyKitchen(OrderEntity saved, List<OrderItem> kitchenItems) {
        kitchenItems.forEach(it -> log.info("KitchenItem - menuItemId={}, name={}, quantity={}",
                it.getMenuItemId(), it.getMenuItemName(), it.getQuantity()));
        if (saved == null) return;

        List<Map<String, Object>> itemsForKitchen = kitchenItems.stream().map(it -> {
            Map<String, Object> m = new HashMap<>();
            m.put("menuItemId", it.getMenuItemId() != null ? it.getMenuItemId().toString() : null);
            m.put("menuItemName", it.getMenuItemName());
            m.put("quantity", it.getQuantity());
            return m;
        }).collect(Collectors.toList());

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(itemsForKitchen);
        } catch (Exception ex) {
            log.error("Failed to serialize kitchen items for order {}: {}", saved.getId(), ex.getMessage(), ex);
            saved.setKitchenStatus("kitchen_notify_failed");
            orderRepository.save(saved);
            return;
        }

        KitchenClient.KitchenOrderRequest req = new KitchenClient.KitchenOrderRequest();
        req.orderId = saved.getId();
        req.itemsJson = itemsJson;

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("orderId", req.orderId);
        payloadMap.put("itemsJson", req.itemsJson);

        try {
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            log.debug("Kitchen request JSON for order {}: {}", saved.getId(), payloadJson);
        } catch (Exception e) {
            // fallback, but map serialization should never fail
            log.debug("Kitchen request values for order {}: orderId={}, itemsJson={}", saved.getId(), req.orderId, req.itemsJson);
        }

        log.debug("Sending kitchen order payload for order {}: {}", saved.getId(), itemsJson);


        try {
            KitchenClient.KitchenOrderResponse resp = kitchenClient.createKitchenOrder(req);
            if (resp != null && resp.id != null) {
                saved.setKitchenOrderId(resp.id);
                saved.setKitchenStatus(resp.status);
                orderRepository.save(saved);
                log.info("Kitchen order created: {} for order {}", resp.id, saved.getId());
            } else {
                log.warn("Kitchen client returned null/empty response for order {}", saved.getId());
                saved.setKitchenStatus("kitchen_notify_failed");
                orderRepository.save(saved);
            }
        } catch (FeignException fe) {
            String body = "";
            try { body = fe.contentUTF8(); } catch (Exception ignored) {}
            log.error("Failed to notify kitchen service for order {}: status={}, body={}", saved.getId(), fe.status(), body, fe);
            saved.setKitchenStatus("kitchen_notify_failed");
            orderRepository.save(saved);
        } catch (Exception ex) {
            log.error("Unexpected error when notifying kitchen for order {}: {}", saved.getId(), ex.getMessage(), ex);
            saved.setKitchenStatus("kitchen_notify_failed");
            orderRepository.save(saved);
        }
    }
}