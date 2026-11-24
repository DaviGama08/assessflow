package com.davigama.assessflow.locallive.domain;

public final class JoinUrls {
    private JoinUrls() {}

    public static String join(String host, int port, String code) {
        String hostname = host == null || host.isBlank() ? "127.0.0.1" : host.replaceAll("/$", "");
        String normalized = code == null ? "" : code.trim().toUpperCase();
        return "http://" + hostname + ":" + port + "/join/" + normalized;
    }
}
