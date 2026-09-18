package com.davigama.assessflow.locallive.api;

import java.util.List;
import java.util.UUID;

public final class LocalLiveDtos {
    private LocalLiveDtos() {}

    public record StatusResponse(String mode, boolean internetRequired, String selectedHost, int port,
                                 List<String> lanAddresses, String joinBaseUrl, String hotspotStatus,
                                 String captivePortal, String database, String websocket) {}

    public record HotspotResponse(boolean available, String ssid, String wifiQr, String message) {}

    public record PackageImportResponse(UUID organizationId, UUID assessmentId, String title) {}
}
