package org.example.main.repository;

import org.example.main.model.MenuItem;
import org.example.main.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository with finder methods used by the scheduler.
 *
 * Notes:
 * - The native query below uses the DB table/column names observed in your Hibernate logs:
 *   menu_items, order_items, orders, order_items.menu_item_id, order_items.order_id, orders.created_at.
 *   If your DB schema uses different names, update the SQL accordingly.
 *
 * - The JPQL fetch-join here only fetches order -> items to avoid depending on the OrderItem->MenuItem
 *   property name in JPQL. We use a native query to retrieve distinct MenuItem entities sold in the date range
 *   to avoid the "oi.menuItem" JPQL path resolution issue.
 */
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /**
     * Find orders created between two instants (used by the daily report).
     * If your OrderEntity.createdAt uses OffsetDateTime/LocalDateTime change the parameter type accordingly.
     */
    List<OrderEntity> findByCreatedAtBetween(Instant from, Instant to);

    /**
     * Fetch-join orders with their items to avoid lazy-loading the items collection.
     * This avoids an N+1 when the scheduler iterates order.getItems().
     *
     * NOTE: This query does not attempt to join the menu item relation on OrderItem because
     * the property name on OrderItem for the menu item was unknown and caused JPQL errors.
     * If your OrderItem has a property named 'menuItem', and you want to fetch it eagerly as well,
     * replace this method with a fetch-join including that path (e.g. "left join fetch oi.menuItem mi").
     */
    @Query("select distinct o from OrderEntity o " +
            "left join fetch o.items oi " +
            "where o.createdAt between :from and :to")
    List<OrderEntity> findWithItemsByCreatedAtBetween(@Param("from") Instant from,
                                                      @Param("to") Instant to);

    /**
     * Native query returning distinct MenuItem entities that appear in orders between the given instants.
     * This avoids JPQL path resolution issues (oi.menuItem) by using concrete DB table/column names.
     *
     * Returns a List<MenuItem> mapped by JPA — MenuItem must be a mapped entity.
     *
     * Adjust table/column names if your schema differs.
     */
    @Query(value = "SELECT DISTINCT mi.* " +
            "FROM menu_items mi " +
            "JOIN order_items oi ON oi.menu_item_id = mi.id " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.created_at BETWEEN :from AND :to",
            nativeQuery = true)
    List<MenuItem> findMenuItemsByCreatedAtBetweenNative(@Param("from") Instant from,
                                                         @Param("to") Instant to);

    /**
     * Find orders whose status is among the provided list (used for "open" orders in periodic checks).
     * If you use an enum type for status, change the parameter type accordingly (e.g. List<OrderStatus>).
     */
    List<OrderEntity> findByStatusIn(List<String> statuses);
}