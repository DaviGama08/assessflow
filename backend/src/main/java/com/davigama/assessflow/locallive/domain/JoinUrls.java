package com.davigama.assessflow.locallive.domain;

public final class JoinUrls {
    private JoinUrls() {}

    public static String join(String host, int port, String code) {
        String hostname = host == null || host.isBlank() ? "127.0.0.1" : host.replaceAll("/$", "");
        String normalized = code == null ? "" : code.trim().toUpperCase();
        return "http://" + hostname + ":" + port + "/join/" + normalized;
    }

    public static String wifi(String ssid, String password) {
        return "WIFI:T:WPA;S:" + escape(ssid) + ";P:" + escape(password) + ";;";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:");
    }
}
