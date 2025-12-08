package org.example.main.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    NEW("New"),
    PROCESSING("Processing"),
    READY("Ready"),
    COMPLETED("Completed"),
    PAID("Paid"),
    CANCELLED("Cancelled");


    private final String label;

    OrderStatus(String label) {
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
    public static OrderStatus fromLabel(String label) {
        if (label == null) throw new IllegalArgumentException("label is null");
        for (OrderStatus s : values()) {
            if (s.label.equalsIgnoreCase(label.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown OrderStatus: " + label);
    }

    public boolean isActive() {
        return this != COMPLETED && this != CANCELLED && this != PAID;
    }
}