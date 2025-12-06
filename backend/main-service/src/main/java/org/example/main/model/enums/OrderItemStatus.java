package org.example.main.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderItemStatus {
    PENDING("Pending"),
    PREPARING("Preparing"),
    READY("Ready"),
    SERVED("Served"),
    CANCELLED("Cancelled");

    private final String label;

    OrderItemStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    @JsonCreator
    public static OrderItemStatus fromLabel(String label) {
        if (label == null) throw new IllegalArgumentException("label is null");
        for (OrderItemStatus s : values()) {
            if (s.label.equalsIgnoreCase(label.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown OrderStatus: " + label);
    }
}


