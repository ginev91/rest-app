package org.example.main.repository;

import org.example.main.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}