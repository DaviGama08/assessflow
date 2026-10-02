package com.davigama.assessflow.locallive.domain;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public final class LanAddresses {
    private LanAddresses() {}

    public static boolean privateIpv4(String host) {
        if (host == null || host.isBlank()) return false;
        String value = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(value) || "127.0.0.1".equals(value)) return true;
        String[] parts = value.split("\\.");
        if (parts.length != 4) return false;
        try {
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            return a == 10
                    || (a == 192 && b == 168)
                    || (a == 172 && b >= 16 && b <= 31);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean allowedOrigin(String origin, int serverPort) {
        if (origin == null || origin.isBlank()) return true;
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
            return privateIpv4(host) && port == serverPort;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static List<String> discover() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks != null && networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                String name = network.getName().toLowerCase(Locale.ROOT);
                if (name.startsWith("docker") || name.startsWith("br-") || name.startsWith("veth")
                        || name.contains("virbr") || name.startsWith("tun") || name.startsWith("wg")) {
                    continue;
                }
                Enumeration<InetAddress> inet = network.getInetAddresses();
                while (inet.hasMoreElements()) {
                    InetAddress address = inet.nextElement();
                    if (address instanceof Inet4Address ipv4 && privateIpv4(ipv4.getHostAddress())
                            && !ipv4.isLoopbackAddress()) {
                        addresses.add(ipv4.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
            // no usable interfaces
        }
        return addresses;
    }
}
