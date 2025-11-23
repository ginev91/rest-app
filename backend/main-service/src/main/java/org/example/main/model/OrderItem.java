package org.example.main.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private int quantity;

    // link to menu item entity (optional). Keep LAZY to avoid eager loading.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = true)
    private MenuItem menuItem;

    // snapshot of menu item name and price at order time:
    @Column(name = "menu_item_name", nullable = false)
    private String menuItemName;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    // owning side for order relationship
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    // Convenience getters to expose menuItem id without forcing menuItem fetch
    public UUID getMenuItemId() {
        return (this.menuItem != null) ? this.menuItem.getId() : null;
    }

    @PrePersist
    @PreUpdate
    private void syncSnapshotFields() {
        if (this.menuItem != null) {
            if (this.menuItemName == null || this.menuItemName.isBlank()) {
                this.menuItemName = this.menuItem.getName();
            }
            if (this.price == null) {
                this.price = this.menuItem.getPrice();
            }
        }

        if (this.menuItemName == null) this.menuItemName = "";
        if (this.price == null) this.price = BigDecimal.ZERO;
    }
}