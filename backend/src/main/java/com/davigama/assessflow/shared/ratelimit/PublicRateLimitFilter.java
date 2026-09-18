package com.davigama.assessflow.shared.ratelimit;

import com.davigama.assessflow.shared.observability.AssessFlowMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PublicRateLimitFilter extends OncePerRequestFilter {
    private final PublicRequestRateLimiter limiter;
    private final AssessFlowMetrics metrics;
    private final boolean trustedProxy;
    private final int joinPerMinute;
    private final int previewPerMinute;
    private final int authPerMinute;

    public PublicRateLimitFilter(
            PublicRequestRateLimiter limiter,
            AssessFlowMetrics metrics,
            @Value("${app.http.trusted-proxy:false}") boolean trustedProxy,
            @Value("${app.rate-limit.join-per-minute:30}") int joinPerMinute,
            @Value("${app.rate-limit.preview-per-minute:60}") int previewPerMinute,
            @Value("${app.rate-limit.auth-per-minute:20}") int authPerMinute) {
        this.limiter = limiter;
        this.metrics = metrics;
        this.trustedProxy = trustedProxy;
        this.joinPerMinute = joinPerMinute;
        this.previewPerMinute = previewPerMinute;
        this.authPerMinute = authPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit != null && !limiter.allow(limit.bucket, clientKey(request), limit.perMinute, 60)) {
            metrics.rateLimitRejected();
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"title\":\"Too Many Requests\",\"status\":429,\"code\":\"RATE_LIMITED\","
                            + "\"detail\":\"Too many requests. Retry shortly.\"}");
            return;
        }
        if (limit != null) {
            metrics.rateLimitAllowed();
        }
        filterChain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/api/v1/live-sessions/join")) {
            return new Limit("join", joinPerMinute);
        }
        if ("GET".equalsIgnoreCase(method) && path.contains("/api/v1/live-sessions/preview")) {
            return new Limit("preview", previewPerMinute);
        }
        if ("POST".equalsIgnoreCase(method) && (path.endsWith("/api/v1/auth/login")
                || path.endsWith("/api/v1/auth/register"))) {
            return new Limit("auth", authPerMinute);
        }
        return null;
    }

    /**
     * Remote address is hashed so rate-limit keys never store raw IPs or other identifiers.
     * X-Forwarded-For is ignored unless app.http.trusted-proxy=true, which should only be set
     * behind a known reverse proxy (Cloudflare, Azure, nginx) that overwrites forwarded headers.
     */
    private String clientKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (trustedProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                ip = forwarded.split(",")[0].trim();
            }
        }
        return ClientKeys.hash(ip);
    }

    private record Limit(String bucket, int perMinute) {}
}
