package org.example.main.repository;

import org.example.main.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for OrderItem entities.
 *
 * Note: this repository assumes your OrderItem entity has associations named:
 *   - private MenuItem menuItem;   // use menuItem.id in queries
 *   - private Order order;         // use order.id in queries
 *
 * If your OrderItem field names differ (for example menuItemId as a UUID field),
 * update method names accordingly (e.g. findByMenuItemId).
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}