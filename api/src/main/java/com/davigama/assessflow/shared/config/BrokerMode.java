package com.davigama.assessflow.shared.config;

public enum BrokerMode {
    SIMPLE,
    RELAY;

    public static BrokerMode from(String value) {
        if (value == null || value.isBlank()) {
            return SIMPLE;
        }
        try {
            return BrokerMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid app.realtime.broker-mode '" + value
                    + "'. Allowed values: simple, relay.");
        }
    }
}
