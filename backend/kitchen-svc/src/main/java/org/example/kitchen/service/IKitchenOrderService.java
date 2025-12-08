package org.example.kitchen.service;

import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.enums.KitchenOrderStatus;

import java.util.List;
import java.util.UUID;

public interface IKitchenOrderService {
    KitchenOrder createOrder(UUID orderId, String itemsJson);
    KitchenOrder updateStatus(UUID id, KitchenOrderStatus status);
    void cancelOrder(UUID id);
    List<KitchenOrder> findByOrderId(UUID orderId);
    void delete(UUID id);
}