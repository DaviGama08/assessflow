package com.davigama.assessflow.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieSettings {
    private final boolean secure;
    private final String sameSite;

    public RefreshCookieSettings(
            @Value("${app.auth.secure-cookie:true}") boolean secure,
            @Value("${app.auth.refresh-cookie.same-site:Strict}") String sameSite) {
        this.secure = secure;
        this.sameSite = normalize(sameSite);
        if ("None".equalsIgnoreCase(this.sameSite) && !this.secure) {
            throw new IllegalStateException("app.auth.refresh-cookie.same-site=None requires app.auth.secure-cookie=true.");
        }
    }

    public boolean secure() {
        return secure;
    }

    public String sameSite() {
        return sameSite;
    }

    public ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("assessflow_refresh", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }

    static String normalize(String sameSite) {
        if (sameSite == null || sameSite.isBlank()) {
            return "Strict";
        }
        String value = sameSite.trim();
        if ("Strict".equalsIgnoreCase(value)) {
            return "Strict";
        }
        if ("Lax".equalsIgnoreCase(value)) {
            return "Lax";
        }
        if ("None".equalsIgnoreCase(value)) {
            return "None";
        }
        throw new IllegalStateException(
                "Invalid app.auth.refresh-cookie.same-site '" + sameSite + "'. Allowed values: Strict, Lax, None.");
    }
}
