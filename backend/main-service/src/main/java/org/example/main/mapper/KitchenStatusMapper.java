package org.example.main.mapper;

import org.example.main.model.enums.OrderItemStatus;
import org.example.main.model.enums.OrderStatus;

import java.util.Locale;
import java.util.Objects;

/**
 * Map kitchen-service status values (strings) into Main application enums.
 * Place this class in the Main application: src/main/java/org/example/main/mapper
 */
public final class KitchenStatusMapper {

    private KitchenStatusMapper() { }

    public static OrderItemStatus toOrderItemStatus(String kitchenStatus) {
        if (kitchenStatus == null) return null;
        String s = kitchenStatus.trim().toUpperCase(Locale.ROOT);
        if (s.equals("CANCELED")) s = "CANCELLED";

        switch (s) {
            case "NEW":
            case "PENDING":
                return OrderItemStatus.PENDING;

            case "PREPARING":
            case "IN_PROGRESS":
            case "INPROGRESS":
                return OrderItemStatus.PREPARING;

            case "READY":
                return OrderItemStatus.READY;

            case "SERVED":
            case "COMPLETED":
                return OrderItemStatus.SERVED;

            case "CANCELLED":
                return OrderItemStatus.CANCELLED;

            default:
                return null;
        }
    }

    public static OrderStatus toOrderStatus(String kitchenStatus) {
        if (kitchenStatus == null) return null;
        String s = kitchenStatus.trim().toUpperCase(Locale.ROOT);
        if (s.equals("CANCELED")) s = "CANCELLED";

        switch (s) {
            case "NEW":
                return OrderStatus.NEW;
            case "PREPARING":
            case "IN_PROGRESS":
                return OrderStatus.PROCESSING;
            case "READY":
                return OrderStatus.READY;
            case "SERVED":
            case "COMPLETED":
                return OrderStatus.COMPLETED;
            case "CANCELLED":
                return OrderStatus.CANCELLED;
            default:
                return null;
        }
    }

    public static OrderItemStatus toOrderItemStatusOrDefault(String kitchenStatus) {
        OrderItemStatus s = toOrderItemStatus(kitchenStatus);
        return s == null ? OrderItemStatus.PENDING : s;
    }

    public static OrderStatus toOrderStatusOrDefault(String kitchenStatus) {
        OrderStatus s = toOrderStatus(kitchenStatus);
        return s == null ? OrderStatus.PROCESSING : s;
    }

    public static boolean isTerminal(String kitchenStatus) {
        OrderStatus os = toOrderStatus(kitchenStatus);
        return Objects.equals(os, OrderStatus.COMPLETED) || Objects.equals(os, OrderStatus.CANCELLED);
    }
}