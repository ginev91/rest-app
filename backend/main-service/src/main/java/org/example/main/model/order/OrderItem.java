package org.example.main.model.order;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;

import org.example.main.model.enums.ItemType;
import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.menu.MenuItem;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = true)
    private MenuItem menuItem;

    @Column(name = "menu_item_name", nullable = false)
    private String menuItemName;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    public UUID getMenuItemId() {
        return (this.menuItem != null) ? this.menuItem.getId() : null;
    }

    @PrePersist
    @PreUpdate
    private void syncSnapshotFields() {
        if (this.menuItem != null) {
            // copy snapshot fields from menuItem when available
            if (this.menuItemName == null || this.menuItemName.isBlank()) {
                String name = this.menuItem.getName();
                this.menuItemName = name != null ? name : "";
            }
            if (this.price == null) {
                BigDecimal mp = this.menuItem.getPrice();
                this.price = mp != null ? mp : BigDecimal.ZERO;
            }
        }

        if (this.menuItemName == null) this.menuItemName = "";
        if (this.price == null) this.price = BigDecimal.ZERO;
    }

    public boolean isKitchenItem() {
        if (this.menuItem == null) return false;
        ItemType type = this.menuItem.getItemType();
        return type == ItemType.KITCHEN || type == null;
    }


    public void setMenuItemId(UUID uuid) {
        if (this.menuItem == null) {
            this.menuItem = new MenuItem();
        }
        this.menuItem.setId(uuid);
    }
}