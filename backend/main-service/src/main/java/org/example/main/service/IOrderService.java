package org.example.main.service;

import aj.org.objectweb.asm.commons.Remapper;
import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;
import org.example.main.model.enums.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IOrderService {


    OrderResponseDto createOrder(OrderRequestDto request);

    OrderResponseDto getOrderSummary(UUID orderId);

    OrderDetailsResponseDto getOrderDetails(UUID orderId);

    List<OrderResponseDto> getOrdersForUser(UUID userId);

    UUID placeOrder(OrderRequestDto dto);

    void cancelOrder(UUID orderId);

    void updateStatus(UUID orderId, OrderStatus status);

    List<OrderResponseDto> getOrdersForTable(UUID tableId);

    List<OrderResponseDto> getAllOrders();

    Optional <OrderResponseDto> getActiveOrderForUser(UUID userId);
}