package com.davigama.assessflow.shared.health;

import com.davigama.assessflow.shared.config.RealtimeSettings;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.realtime.broker-mode", havingValue = "relay")
public class StompRelayHealthIndicator implements HealthIndicator {
    private final RealtimeSettings realtime;

    public StompRelayHealthIndicator(RealtimeSettings realtime) {
        this.realtime = realtime;
    }

    @Override
    public Health health() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(realtime.relayHost(), realtime.relayPort()), 1000);
            return Health.up().withDetail("broker", "relay").build();
        } catch (Exception ex) {
            return Health.down().withDetail("broker", "relay").withException(ex).build();
        }
    }
}
