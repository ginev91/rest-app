package org.example.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.main.dto.kitchen.KitchenInfoDto;
import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.request.OrderRequestDto.OrderItemRequest;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderItemResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.feign.KitchenClient;
import org.example.main.mapper.KitchenResponseMapper;
import org.example.main.mapper.KitchenStatusMapper;
import org.example.main.model.MenuItem;
import org.example.main.model.OrderEntity;
import org.example.main.model.OrderItem;
import org.example.main.model.User;
import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.enums.OrderStatus;
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

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        // Validate menu items exist
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

        UUID customerId = request.getCustomerId();

        // Build items for the incoming request (detached)
        List<OrderItem> newItems = new ArrayList<>();
        BigDecimal newItemsTotal = BigDecimal.ZERO;
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
            oi.setStatus(OrderItemStatus.PENDING);
            oi.setPrice(mi.getPrice() != null ? mi.getPrice() : BigDecimal.ZERO);
            // order is set later when attaching to an order entity
            newItems.add(oi);

            BigDecimal line = oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity()));
            newItemsTotal = newItemsTotal.add(line);
        }

        // Check for an existing active order for this customer.
        // NOTE: Active = any order that is NOT CANCELLED. (COMPLETED orders are eligible to receive new items.)
        Optional<OrderEntity> activeOpt = Optional.empty();
        if (customerId != null) {
            activeOpt = orderRepository.findWithItemsByCustomerUserId(customerId)
                    .stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED) // only exclude CANCELLED
                    .findFirst();
        }

        if (activeOpt.isPresent()) {
            // Merge into existing active order
            OrderEntity existing = activeOpt.get();
            if (existing.getItems() == null) existing.setItems(new ArrayList<>());

            // Attach each new item to the existing order
            for (OrderItem ni : newItems) {
                ni.setOrder(existing);
                existing.getItems().add(ni);
            }

            // Update totals and timestamps
            existing.setTotalAmount(existing.getTotalAmount() == null ? newItemsTotal : existing.getTotalAmount().add(newItemsTotal));
            existing.setUpdatedAt(OffsetDateTime.now());

            // If the existing order was in a terminal-not-cancelled state (e.g., COMPLETED or READY),
            // moving it to PROCESSING reflects that new kitchen work is required.
            if (existing.getStatus() == OrderStatus.COMPLETED || existing.getStatus() == OrderStatus.READY || existing.getStatus() == OrderStatus.NEW) {
                existing.setStatus(OrderStatus.PROCESSING);
            } else {
                // For other statuses (e.g., PROCESSING), keep as-is (still in-progress).
                existing.setStatus(OrderStatus.PROCESSING);
            }

            // Persist merged order
            OrderEntity saved = orderRepository.save(existing);
            log.info("Merged {} new items into existing active order {} for customer={}", newItems.size(), saved.getId(), customerId);

            // Notify kitchen only for the newly added kitchen items
            List<OrderItem> kitchenItems = newItems.stream()
                    .filter(it -> {
                        MenuItem mi = it.getMenuItem();
                        if (mi == null) return false;
                        ItemType type = mi.getItemType();
                        return type == ItemType.KITCHEN || type == null;
                    })
                    .collect(Collectors.toList());

            if (!kitchenItems.isEmpty()) {
                notifyKitchen(saved, kitchenItems);
            }

            return OrderResponseDto.builder()
                    .orderId(saved.getId())
                    .status(String.valueOf(saved.getStatus()))
                    .build();
        }

        // No active order -> create a new one (existing behavior)
        OrderEntity order = new OrderEntity();
        order.setTableId(request.getTableId());
        order.setTableNumber(request.getTableNumber());
        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(findUserName(request.getCustomerId()));
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        // Attach items to the new order
        for (OrderItem it : newItems) {
            it.setOrder(order);
        }
        order.setItems(newItems);
        order.setTotalAmount(newItemsTotal);

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
                .status(String.valueOf(saved.getStatus()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderSummary(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return OrderResponseDto.builder()
                .orderId(o.getId())
                .status(String.valueOf(o.getStatus()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailsResponseDto getOrderDetails(UUID orderId) {
        // Load from DB
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // Enrich with kitchen service data if available
        try {
            List<org.example.main.feign.KitchenClient.KitchenOrderResponse> kitchenOrders = kitchenClient.getByOrder(orderId);
            if (kitchenOrders != null && !kitchenOrders.isEmpty()) {
                org.example.main.feign.KitchenClient.KitchenOrderResponse koResp = kitchenOrders.stream()
                        .filter(ko -> o.getKitchenOrderId() != null && o.getKitchenOrderId().equals(ko.id))
                        .findFirst()
                        .orElse(kitchenOrders.get(0));

                KitchenInfoDto ki = KitchenResponseMapper.toKitchenInfo(koResp);
                if (ki != null) {
                    o.setKitchenOrderId(ki.getKitchenOrderId());
                    o.setKitchenStatus(ki.getStatus());

                    // map kitchen status to item statuses using KitchenStatusMapper
                    OrderItemStatus mapped = KitchenStatusMapper.toOrderItemStatus(ki.getStatus());
                    if (mapped != null && o.getItems() != null) {
                        for (OrderItem it : o.getItems()) {
                            if (it.isKitchenItem()) {
                                it.setStatus(mapped);
                            }
                        }
                    }
                }
            }
        } catch (FeignException fe) {
            log.warn("Failed to fetch kitchen orders for {}: {}", orderId, fe.status());
        } catch (Exception ex) {
            log.error("Error while calling kitchen service for order {}: {}", orderId, ex.getMessage(), ex);
        }

        return mapEntityToDetailsDto(o);
    }

    @Override
    @Transactional
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
    public void cancelOrder(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // local cancellation first for immediate user feedback
        o.setStatus(OrderStatus.CANCELLED);
        o.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(o);

        UUID kitchenOrderId = o.getKitchenOrderId();
        if (kitchenOrderId != null) {
            try {
                // prefer dedicated cancel endpoint on kitchen service
                kitchenClient.cancelKitchenOrder(kitchenOrderId);
                o.setKitchenStatus("CANCELLED");
                orderRepository.save(o);
                log.info("Notified kitchen to cancel kitchenOrderId={}", kitchenOrderId);
            } catch (FeignException fe) {
                String body = "";
                try { body = fe.contentUTF8(); } catch (Exception ignored) {}
                log.error("Failed to notify kitchen to cancel {}: status={}, body={}", kitchenOrderId, fe.status(), body, fe);
                o.setKitchenStatus("cancel_notify_failed");
                orderRepository.save(o);
            } catch (Exception ex) {
                log.error("Unexpected error notifying kitchen: {}", ex.getMessage(), ex);
                o.setKitchenStatus("cancel_notify_failed");
                orderRepository.save(o);
            }
        }
    }

    @Override
    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status) {
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

    @Override
    public Optional<OrderResponseDto> getActiveOrderForUser(UUID userId) {
        return orderRepository.findWithItemsByCustomerUserId(userId)
                .stream()
                .filter(o -> o.getStatus() != OrderStatus.COMPLETED && o.getStatus() != OrderStatus.CANCELLED)
                .map(this::mapToOrderResponseDto)
                .findFirst();
    }

    private String findUserName(UUID userId) {
        if (userId == null) return null;
        Optional<User> u = userRepository.findById(userId);
        return u.map(User::getFullName).orElse(null);
    }

    private OrderDetailsResponseDto mapEntityToDetailsDto(OrderEntity o) {
        List<OrderItemResponseDto> items = (o.getItems() == null) ? Collections.emptyList() :
                o.getItems().stream()
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
                .status(String.valueOf(o.getStatus()))
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
                                .status(it.getStatus())
                                .price(it.getPrice())
                                .build())
                        .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(e.getId())
                .status(String.valueOf(e.getStatus()))
                .totalAmount(e.getTotalAmount())
                .tableNumber(e.getTableNumber())
                .createdAt(e.getCreatedAt())
                .items(items)
                .username(e.getCustomerName())
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

        OrderItemStatus mappedItemStatus = KitchenStatusMapper.toOrderItemStatus(kitchenStatus);

        boolean allKitchen = true;
        if (order.getItems() == null || order.getItems().isEmpty()) {
            allKitchen = false;
        } else {
            for (OrderItem item : order.getItems()) {
                boolean kitchenItem;
                try {
                    kitchenItem = item.isKitchenItem();
                } catch (NoSuchMethodError | AbstractMethodError ex) {
                    kitchenItem = false;
                }

                if (kitchenItem) {
                    if (mappedItemStatus != null) {
                        item.setStatus(mappedItemStatus);
                    } else {
                        if ("READY".equalsIgnoreCase(kitchenStatus)) {
                            item.setStatus(OrderItemStatus.READY);
                        }
                    }
                } else {
                    allKitchen = false;
                }

                if (!allKitchen) {
                    break;
                }
            }
        }

        if (allKitchen && KitchenStatusMapper.isTerminal(kitchenStatus)) {
            order.setStatus(KitchenStatusMapper.toOrderStatusOrDefault(kitchenStatus));
        } else if (allKitchen && "READY".equalsIgnoreCase(kitchenStatus)) {
            order.setStatus(OrderStatus.COMPLETED);
        }

        orderRepository.save(order);
        log.info("Order {} kitchenStatus updated to {} (kitchenOrderId={})", orderId, kitchenStatus, kitchenOrderId);

    }

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
            org.example.main.feign.KitchenClient.KitchenOrderResponse resp = kitchenClient.createKitchenOrder(req);
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