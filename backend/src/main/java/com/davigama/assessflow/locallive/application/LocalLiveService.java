package com.davigama.assessflow.locallive.application;

import com.davigama.assessflow.locallive.api.LocalLiveDtos.HotspotResponse;
import com.davigama.assessflow.locallive.api.LocalLiveDtos.StatusResponse;
import com.davigama.assessflow.locallive.domain.JoinUrls;
import com.davigama.assessflow.locallive.domain.LanAddresses;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.local-live.enabled", havingValue = "true")
public class LocalLiveService {
    private static final Logger log = LoggerFactory.getLogger(LocalLiveService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final String configuredHost;
    private final int port;
    private final Path hotspotScript;
    private volatile HotspotResponse hotspot = new HotspotResponse(false, null, null,
            "Automatic hotspot unavailable. Connect all devices to the same Wi-Fi network and use the Local Join QR.");

    public LocalLiveService(@Value("${app.local-live.host:}") String configuredHost,
                            @Value("${server.port:8080}") int port,
                            @Value("${app.local-live.hotspot-script:}") String hotspotScript) {
        this.configuredHost = configuredHost;
        this.port = port;
        this.hotspotScript = hotspotScript == null || hotspotScript.isBlank() ? null : Path.of(hotspotScript);
    }

    public StatusResponse status() {
        List<String> addresses = LanAddresses.discover();
        String selected = selectedHost(addresses);
        log.info("Local Live status selected LAN address {}", selected);
        return new StatusResponse("LOCAL", false, selected, port, addresses, JoinUrls.join(selected, port, ""),
                hotspot.available() ? "Active" : "Unavailable", "Best effort", "Local PostgreSQL", "STOMP /ws");
    }

    public String joinUrl(String code) {
        return JoinUrls.join(selectedHost(LanAddresses.discover()), port, code);
    }

    public HotspotResponse hotspot() {
        return hotspot;
    }

    public HotspotResponse startHotspot() {
        String ssid = "AssessFlow-" + HexFormat.of().withUpperCase().formatHex(random(2));
        String password = HexFormat.of().formatHex(random(8));
        if (hotspotScript == null || !Files.isExecutable(hotspotScript)) {
            log.info("Local Live hotspot helper missing; using same-LAN fallback");
            hotspot = new HotspotResponse(false, ssid, JoinUrls.wifi(ssid, password),
                    "Automatic hotspot unavailable. Connect all devices to the same Wi-Fi network and use the Local Join QR.");
            return hotspot;
        }
        try {
            Process process = new ProcessBuilder(hotspotScript.toString(), "start", ssid, password)
                    .redirectErrorStream(true).start();
            int code = process.waitFor();
            if (code != 0) {
                hotspot = new HotspotResponse(false, ssid, JoinUrls.wifi(ssid, password),
                        "Hotspot helper failed. Use the Local Join QR on the same Wi-Fi.");
                return hotspot;
            }
            log.info("Local Live hotspot started SSID {}", ssid);
            hotspot = new HotspotResponse(true, ssid, JoinUrls.wifi(ssid, password),
                    "Scan the Wi-Fi QR. AssessFlow should open automatically; if it does not, scan the Join QR.");
            return hotspot;
        } catch (Exception ex) {
            hotspot = new HotspotResponse(false, ssid, JoinUrls.wifi(ssid, password),
                    "Automatic hotspot unavailable. Connect all devices to the same Wi-Fi network and use the Local Join QR.");
            return hotspot;
        }
    }

    @PreDestroy
    public void shutdown() {
        stopHotspot();
    }

    public HotspotResponse stopHotspot() {
        if (hotspotScript != null && Files.isExecutable(hotspotScript)) {
            try {
                new ProcessBuilder(hotspotScript.toString(), "stop").start().waitFor();
                log.info("Local Live hotspot stopped");
            } catch (Exception ignored) {
                // best effort
            }
        }
        hotspot = new HotspotResponse(false, null, null,
                "Automatic hotspot unavailable. Connect all devices to the same Wi-Fi network and use the Local Join QR.");
        return hotspot;
    }

    private String selectedHost(List<String> addresses) {
        if (configuredHost != null && !configuredHost.isBlank()) return configuredHost.trim();
        return addresses.isEmpty() ? "127.0.0.1" : addresses.getFirst();
    }

    private byte[] random(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
