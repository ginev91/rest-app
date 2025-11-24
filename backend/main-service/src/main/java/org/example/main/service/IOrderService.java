package org.example.main.service;

import org.example.main.dto.request.OrderRequestDto;
import org.example.main.dto.response.OrderDetailsResponseDto;
import org.example.main.dto.response.OrderResponseDto;

import java.util.List;
import java.util.UUID;

public interface IOrderService {

    /**
     * Create order and return lightweight response DTO.
     */
    OrderResponseDto createOrder(OrderRequestDto request);

    /**
     * Return an Order summary used in lists.
     */
    OrderResponseDto getOrderSummary(UUID orderId);

    /**
     * Full order details for OrderDetails page.
     */
    OrderDetailsResponseDto getOrderDetails(UUID orderId);

    /**
     * List orders for a given user.
     */
    List<OrderResponseDto> getOrdersForUser(UUID userId);

    /**
     * Convenience wrapper to place an order returning the saved UUID.
     */
    UUID placeOrder(OrderRequestDto dto);

    /**
     * Request waiter call
     */
    void callWaiter(UUID orderId);

    /**
     * Cancel order
     */
    void cancelOrder(UUID orderId);

    /**
     * Update arbitrary status (implement in service if you need generic status transitions)
     */
    void updateStatus(UUID orderId, String status);
}