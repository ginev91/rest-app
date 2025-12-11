package org.example.main.model.order;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import lombok.*;
import org.example.main.model.enums.OrderStatus;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_table_id", columnList = "table_id"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "table_id", columnDefinition = "uuid")
    private UUID tableId;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(name = "customer_id", columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = "waiter_id", columnDefinition = "uuid", nullable = true)
    private UUID waiterId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "kitchen_order_id", columnDefinition = "uuid")
    private UUID kitchenOrderId;

    @Column(name = "kitchen_status")
    private String kitchenStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean claimByWaiter(UUID newWaiterId) {
        if (this.waiterId == null) {
            this.waiterId = newWaiterId;
            this.status = OrderStatus.PROCESSING; 
            return true;
        }
        return false;
    }

    public void assignWaiter(UUID newWaiterId) {
        this.waiterId = newWaiterId;
    }

    public void unassignWaiter() {
        this.waiterId = null;
    }
}