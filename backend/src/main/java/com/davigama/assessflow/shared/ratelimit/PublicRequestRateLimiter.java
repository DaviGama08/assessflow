package com.davigama.assessflow.shared.ratelimit;

public interface PublicRequestRateLimiter {
    boolean allow(String bucket, String clientKey, int limit, int windowSeconds);
}
