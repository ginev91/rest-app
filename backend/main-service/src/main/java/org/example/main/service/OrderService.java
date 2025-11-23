package org.example.main.service;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.request.OrderRequestDto.OrderItemRequest;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderItemResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.model.MenuItem;
import org.example.main.model.OrderEntity;
import org.example.main.model.OrderItem;
import org.example.main.model.User;
import org.example.main.repository.MenuItemRepository;
import org.example.main.repository.OrderRepository;
import org.example.main.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create order and return lightweight response (OrderResponseDto).
     * This preserves your existing contract for POST.
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

        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(findUserName(request.getCustomerId()));
        order.setStatus("preparing");

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

        return OrderResponseDto.builder()
                .orderId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    /**
     * Lightweight summary getter used by controller / other callers.
     */
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

    /**
     * Full details getter returning OrderDetailsResponseDto used by frontend to render full order.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailsResponseDto getOrderDetails(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return mapEntityToDetailsDto(o);
    }

    /**
     * IOrderService implementation: placeOrder returns the saved order id (UUID)
     */
    @Override
    @Transactional
    public UUID placeOrder(OrderRequestDto dto) {
        OrderResponseDto resp = createOrder(dto);
        return resp.getOrderId();
    }

    /**
     * IOrderService implementation: mark order as 'call_waiter'
     */
    @Override
    @Transactional
    public void callWaiter(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        o.setStatus("call_waiter");
        orderRepository.save(o);
    }

    /**
     * IOrderService implementation: cancel the order
     */
    @Override
    @Transactional
    public void cancelOrder(UUID orderId) {
        OrderEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        o.setStatus("cancelled");
        orderRepository.save(o);
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
                .items(items)
                .build();
    }
}