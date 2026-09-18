package com.davigama.assessflow.shared.config;

public enum ProxyMode {
    NONE,
    FORWARDED,
    CLOUDFLARE;

    public static ProxyMode from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return ProxyMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid app.http.proxy-mode '" + value + "'. Allowed values: none, forwarded, cloudflare.");
        }
    }
}
