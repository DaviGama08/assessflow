package com.davigama.assessflow.shared.ratelimit;

import com.davigama.assessflow.shared.config.ProxyMode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Trusted proxy headers are safe only if the origin cannot be bypassed.
 * Do not set cloudflare or forwarded unless Azure (or another origin) rejects
 * traffic that did not pass through the configured edge.
 */
@Component
public class ClientIpResolver {
    private final ProxyMode proxyMode;

    public ClientIpResolver(
            @Value("${app.http.proxy-mode:none}") String proxyMode,
            @Value("${app.http.trusted-proxy:false}") boolean legacyTrustedProxy) {
        ProxyMode mode = ProxyMode.from(proxyMode);
        if (mode == ProxyMode.NONE && legacyTrustedProxy) {
            mode = ProxyMode.FORWARDED;
        }
        this.proxyMode = mode;
    }

    public ProxyMode proxyMode() {
        return proxyMode;
    }

    public String clientKey(HttpServletRequest request) {
        return ClientKeys.hash(clientIp(request));
    }

    public String clientIp(HttpServletRequest request) {
        return switch (proxyMode) {
            case NONE -> remoteAddr(request);
            case FORWARDED -> firstForwarded(request);
            case CLOUDFLARE -> cloudflareConnectingIp(request);
        };
    }

    private String cloudflareConnectingIp(HttpServletRequest request) {
        String connecting = request.getHeader("CF-Connecting-IP");
        if (connecting != null && !connecting.isBlank()) {
            return connecting.trim();
        }
        return remoteAddr(request);
    }

    private String firstForwarded(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return remoteAddr(request);
    }

    private static String remoteAddr(HttpServletRequest request) {
        String addr = request.getRemoteAddr();
        return addr == null || addr.isBlank() ? "unknown" : addr;
    }
}
