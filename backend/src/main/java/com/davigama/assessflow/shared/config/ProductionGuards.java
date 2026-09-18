package com.davigama.assessflow.shared.config;

import java.util.Locale;

/**
 * Fail-fast checks for the production profile. Local, test and local-live never call these.
 */
public final class ProductionGuards {
    private ProductionGuards() {}

    public static void requireExplicitHost(String name, String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(name + " must be set to an explicit hostname in production.");
        }
        String value = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(value) || "127.0.0.1".equals(value) || "::1".equals(value) || "[::1]".equals(value)) {
            throw new IllegalStateException(name + " cannot be localhost in production. Set a real service hostname.");
        }
    }

    public static void requireSecret(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set in production.");
        }
    }

    public static void requireTlsJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("DB_URL must be set in production.");
        }
        String lower = jdbcUrl.toLowerCase(Locale.ROOT);
        if (!(lower.contains("sslmode=require") || lower.contains("sslmode=verify-full")
                || lower.contains("sslmode=verify-ca") || lower.contains("ssl=true"))) {
            throw new IllegalStateException(
                    "Production DB_URL must require TLS (sslmode=require or equivalent).");
        }
    }

    public static void requirePublicOrigin(String name, String origins) {
        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException(name + " must list the production frontend origin.");
        }
        if (origins.contains("*")) {
            throw new IllegalStateException(name + " cannot use a wildcard with credentialed cookies.");
        }
    }

    public static void requireScaledPair(int replicas, boolean redisEnabled, boolean relay) {
        if (replicas >= 2 && (!redisEnabled || !relay)) {
            throw new IllegalStateException(
                    "APP_REPLICAS>=2 requires APP_REDIS_ENABLED=true and APP_REALTIME_BROKER_MODE=relay.");
        }
    }
}
