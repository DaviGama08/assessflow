package com.davigama.assessflow.shared.ratelimit;

import com.davigama.assessflow.shared.observability.AssessFlowMetrics;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisPublicRequestRateLimiter implements PublicRequestRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RedisPublicRequestRateLimiter.class);
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        SCRIPT.setResultType(Long.class);
        SCRIPT.setScriptText("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                  redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                if current > tonumber(ARGV[2]) then
                  return 0
                end
                return 1
                """);
    }

    private final StringRedisTemplate redis;
    private final AssessFlowMetrics metrics;

    public RedisPublicRequestRateLimiter(StringRedisTemplate redis, AssessFlowMetrics metrics) {
        this.redis = redis;
        this.metrics = metrics;
    }

    @Override
    public boolean allow(String bucket, String clientKey, int limit, int windowSeconds) {
        String key = "assessflow:rate:" + bucket + ":" + clientKey;
        try {
            Long allowed = redis.execute(SCRIPT, List.of(key),
                    String.valueOf(windowSeconds), String.valueOf(limit));
            return allowed != null && allowed == 1L;
        } catch (RuntimeException ex) {
            metrics.rateLimitError();
            log.warn("Redis rate limiter failed open for bucket={}", bucket, ex);
            return true;
        }
    }
}
