package com.davigama.assessflow.shared.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryPublicRequestRateLimiter implements PublicRequestRateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean allow(String bucket, String clientKey, int limit, int windowSeconds) {
        long window = System.currentTimeMillis() / Math.max(1, windowSeconds * 1000L);
        String key = bucket + ':' + clientKey + ':' + window;
        Window current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.window != window) {
                return new Window(window, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().window < window);
        }
        return current.count.get() <= limit;
    }

    private record Window(long window, AtomicInteger count) {}
}
