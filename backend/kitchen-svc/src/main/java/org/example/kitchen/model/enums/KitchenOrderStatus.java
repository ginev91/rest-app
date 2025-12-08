package org.example.kitchen.model.enums;


public enum KitchenOrderStatus {
    NEW,
    PREPARING,
    IN_PROGRESS,
    READY,
    SERVED,
    COMPLETED,
    CANCELLED;


    public static KitchenOrderStatus fromString(String value) {
        if (value == null) return null;
        try {
            return KitchenOrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}