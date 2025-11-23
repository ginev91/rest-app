package org.example.main.service;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;

import java.util.UUID;

public interface IOrderService {
    UUID placeOrder(OrderRequestDto dto);
    void callWaiter(UUID orderId);
    void cancelOrder(UUID orderId);

    OrderResponseDto getOrderSummary(UUID orderId);

    OrderDetailsResponseDto getOrderDetails(UUID orderId);
}