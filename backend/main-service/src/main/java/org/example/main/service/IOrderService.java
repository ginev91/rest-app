package org.example.main.service;

import org.example.main.dto.request.OrderRequestDto;

import java.util.UUID;

public interface IOrderService {
    UUID placeOrder(OrderRequestDto dto);
    void callWaiter(UUID orderId);
    void cancelOrder(UUID orderId);
}