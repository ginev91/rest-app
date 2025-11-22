package org.example.main.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    // convenience transient field: not persisted directly, kept in sync with menuItem
    @Transient
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private UUID menuItemId;

    public UUID getMenuItemId() {
        if (this.menuItem != null) {
            return this.menuItem.getId();
        }
        return this.menuItemId;
    }


    public void setMenuItemId(UUID id) {
        this.menuItemId = id;
        if (id == null) {
            this.menuItem = null;
            return;
        }

        try {
            MenuItem ref = new MenuItem();
            ref.setId(id);
            this.menuItem = ref;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @PostLoad
    private void syncMenuItemIdAfterLoad() {
        if (this.menuItem != null) {
            this.menuItemId = this.menuItem.getId();
        }
    }

    @PrePersist
    @PreUpdate
    private void syncMenuItemBeforeSave() {
        if (this.menuItem != null) {
            this.menuItemId = this.menuItem.getId();
        }
    }
}