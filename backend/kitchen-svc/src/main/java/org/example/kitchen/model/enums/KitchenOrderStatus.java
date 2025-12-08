package org.example.kitchen.model.enums;

/**
 * Kitchen-side statuses for kitchen orders.
 * These are the canonical statuses used by the kitchen-service.
 * Keep these values stable because they are communicated to the Main app (as strings).
 */
public enum KitchenOrderStatus {
    NEW,
    PREPARING,
    IN_PROGRESS,
    READY,
    SERVED,
    COMPLETED,
    CANCELLED;

    /**
     * Tolerant parser from arbitrary input (case-insensitive, trims).
     * Returns null if the input cannot be mapped.
     */
    public static KitchenOrderStatus fromString(String value) {
        if (value == null) return null;
        try {
            return KitchenOrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}