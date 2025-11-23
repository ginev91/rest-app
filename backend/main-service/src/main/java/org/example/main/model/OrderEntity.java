package org.example.main.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import lombok.*;
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

    // UUID table id (keeps a reference to a table entity if you have one)
    @Column(name = "table_id", columnDefinition = "uuid")
    private UUID tableId;

    // optional numeric table number used by FE
    @Column(name = "table_number")
    private Integer tableNumber;

    // reference to the customer (user) in your system
    @Column(name = "customer_id", columnDefinition = "uuid")
    private UUID customerId;

    // snapshot of customer/display name for convenience
    @Column(name = "customer_name")
    private String customerName;

    // order status - stored as string
    @Column(nullable = false)
    private String status; // e.g. "preparing", "completed", "pending"

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // total amount snapshot (sum of item price * qty)
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // items - the owning side is OrderItem.order
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
}