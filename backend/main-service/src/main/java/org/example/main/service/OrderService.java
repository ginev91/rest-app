package org.example.main.service;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.feign.BillingClient;
import org.example.main.feign.KitchenClient;
import org.example.main.model.OrderEntity;
import org.example.main.model.OrderItem;
import org.example.main.repository.OrderRepository;
import org.example.main.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService implements IOrderService {

    private final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final KitchenClient kitchenClient;
    private final BillingClient billingClient;

    public OrderService (OrderRepository orderRepository, KitchenClient kitchenClient, BillingClient billingClient) {
        this.orderRepository = orderRepository;
        this.kitchenClient = kitchenClient;
        this.billingClient = billingClient;
    }

    @Override
    @Transactional
    public UUID placeOrder(OrderRequestDto dto) {
        OrderEntity order = new OrderEntity();
        order.setTableId(dto.getTableId());
        order.setCustomerId(dto.getCustomerId());
        order.setStatus("PENDING");
        List<OrderItem> items = dto.getItems().stream().map(i -> {
            OrderItem oi = new OrderItem();
            oi.setMenuItemId(i.getMenuItemId());
            oi.setQuantity(i.getQuantity());
            return oi;
        }).collect(Collectors.toList());
        order.setItems(items);
        order = orderRepository.save(order);
        log.info("Created order {} for table {}", order.getId(), order.getTableId());

        // send to kitchen
        Map<String, Object> kitchenDto = new HashMap<>();
        kitchenDto.put("orderId", order.getId());
        kitchenDto.put("items", dto.getItems());
        kitchenClient.createKitchenOrder(kitchenDto);

        order.setStatus("SENT_TO_KITCHEN");
        orderRepository.save(order);

        // create a bill in billing service asynchronously
        Map<String, Object> billReq = new HashMap<>();
        billReq.put("orderId", order.getId());
        // billing client returns bill id
        try {
            billingClient.createBill(billReq);
        } catch (Exception ex) {
            log.warn("Billing service call failed: {}", ex.getMessage());
        }

        return order.getId();
    }

    @Override
    public void callWaiter(UUID orderId) {
        log.info("Waiter requested for order {}", orderId);
        // create WaiterRequest entity or notify further
    }

    @Override
    @CacheEvict(value = "orders", key = "#orderId")
    public void cancelOrder(UUID orderId) {
        var opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Order not found " + orderId);
        }
        var order = opt.get();
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        // call kitchen to cancel
        kitchenClient.cancelKitchenOrder(orderId);
        log.info("Cancelled order {}", orderId);
    }
}