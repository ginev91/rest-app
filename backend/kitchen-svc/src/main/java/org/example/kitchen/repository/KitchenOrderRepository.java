package org.example.kitchen.repository;

import org.example.kitchen.model.KitchenOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, UUID> {
    List<KitchenOrder> findByOrderId(UUID orderId);
}