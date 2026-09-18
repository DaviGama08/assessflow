package com.davigama.assessflow.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionInfrastructureValidator {
    public ProductionInfrastructureValidator(
            Environment environment,
            RealtimeSettings realtime,
            RefreshCookieSettings cookies,
            @Value("${app.redis.enabled:false}") boolean redisEnabled,
            @Value("${app.redis.host:}") String redisHost,
            @Value("${app.redis.password:}") String redisPassword,
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${app.cors.allowed-origins:}") String corsOrigins,
            @Value("${app.ws.allowed-origins:}") String wsOrigins,
            @Value("${app.deploy.replicas:1}") int replicas,
            @Value("${app.http.proxy-mode:none}") String proxyMode) {
        if (!environment.matchesProfiles("production")) {
            return;
        }
        ProductionGuards.requirePublicOrigin("APP_CORS_ALLOWED_ORIGINS", corsOrigins);
        String ws = wsOrigins == null || wsOrigins.isBlank() ? corsOrigins : wsOrigins;
        ProductionGuards.requirePublicOrigin("APP_WS_ALLOWED_ORIGINS", ws);
        ProductionGuards.requireTlsJdbcUrl(jdbcUrl);
        ProductionGuards.requireScaledPair(replicas, redisEnabled, realtime.relay());
        if (redisEnabled) {
            ProductionGuards.requireExplicitHost("REDIS_HOST", redisHost);
            ProductionGuards.requireSecret("REDIS_PASSWORD", redisPassword);
        }
        if (realtime.relay()) {
            ProductionGuards.requireExplicitHost("RABBITMQ_HOST", realtime.relayHost());
            ProductionGuards.requireSecret("RABBITMQ_USERNAME", realtime.relayUsername());
            ProductionGuards.requireSecret("RABBITMQ_PASSWORD", realtime.relayPassword());
        }
        ProxyMode.from(proxyMode);
        if (!cookies.secure()) {
            throw new IllegalStateException("Production requires app.auth.secure-cookie=true.");
        }
    }
}
