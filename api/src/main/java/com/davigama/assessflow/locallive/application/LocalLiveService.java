package com.davigama.assessflow.locallive.application;

import com.davigama.assessflow.locallive.api.LocalLiveDtos.StatusResponse;
import com.davigama.assessflow.locallive.domain.JoinUrls;
import com.davigama.assessflow.locallive.domain.LanAddresses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.local-live.enabled", havingValue = "true")
public class LocalLiveService {
    private final String configuredHost;
    private final int port;

    public LocalLiveService(@Value("${app.local-live.host:}") String configuredHost,
                            @Value("${server.port:8080}") int port) {
        this.configuredHost = configuredHost;
        this.port = port;
    }

    public StatusResponse status() {
        var addresses = LanAddresses.discover();
        String host = configuredHost.isBlank() ? addresses.stream().findFirst().orElse("127.0.0.1") : configuredHost;
        return new StatusResponse("LOCAL", false, host, port, addresses, JoinUrls.join(host, port, ""),
                "Local PostgreSQL", "STOMP /ws");
    }

    public String joinUrl(String code) {
        return JoinUrls.join(status().selectedHost(), port, code);
    }
}
