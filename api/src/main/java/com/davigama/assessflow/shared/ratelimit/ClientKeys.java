package com.davigama.assessflow.shared.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class ClientKeys {
    private ClientKeys() {}

    public static String hash(String value) {
        String material = value == null || value.isBlank() ? "unknown" : value.trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash rate-limit key.", ex);
        }
    }
}
