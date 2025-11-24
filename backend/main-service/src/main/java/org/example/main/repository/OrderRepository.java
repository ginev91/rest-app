package org.example.main.repository;

import org.example.main.model.MenuItem;
import org.example.main.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);


    @Query("select distinct o from OrderEntity o " +
            "left join fetch o.items oi " +
            "where o.createdAt between :from and :to")
    List<OrderEntity> findWithItemsByCreatedAtBetween(@Param("from") OffsetDateTime from,
                                                      @Param("to") OffsetDateTime to);

    @Query(value = "SELECT DISTINCT mi.* " +
            "FROM menu_items mi " +
            "JOIN order_items oi ON oi.menu_item_id = mi.id " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.created_at BETWEEN :from AND :to",
            nativeQuery = true)
    List<MenuItem> findMenuItemsByCreatedAtBetweenNative(@Param("from") OffsetDateTime from,
                                                         @Param("to") OffsetDateTime to);

    /**
     * Find orders whose status is among the provided list (used for "open" orders in periodic checks).
     */
    List<OrderEntity> findByStatusIn(List<String> statuses);

    @Query("select distinct o from OrderEntity o " +
            "left join fetch o.items oi " +
            "where o.customerId = :userId")
    List<OrderEntity> findWithItemsByCustomerUserId(@Param("userId") UUID userId);

    @Query("select distinct o from OrderEntity o " +
            "left join fetch o.items oi " +
            "where o.waiterId = :userId")
    List<OrderEntity> findWithItemsByWaiterUserId(@Param("userId") UUID userId);
}