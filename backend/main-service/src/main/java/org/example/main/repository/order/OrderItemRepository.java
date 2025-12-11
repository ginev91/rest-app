package org.example.main.repository.order;

import org.example.main.model.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}