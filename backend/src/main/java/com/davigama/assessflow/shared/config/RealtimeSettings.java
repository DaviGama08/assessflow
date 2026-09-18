package com.davigama.assessflow.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RealtimeSettings {
    private final BrokerMode brokerMode;
    private final String relayHost;
    private final int relayPort;
    private final String relayUsername;
    private final String relayPassword;
    private final String relayVirtualHost;

    public RealtimeSettings(
            @Value("${app.realtime.broker-mode:simple}") String brokerMode,
            @Value("${app.realtime.relay.host:localhost}") String relayHost,
            @Value("${app.realtime.relay.port:61613}") int relayPort,
            @Value("${app.realtime.relay.username:}") String relayUsername,
            @Value("${app.realtime.relay.password:}") String relayPassword,
            @Value("${app.realtime.relay.virtual-host:/}") String relayVirtualHost) {
        this.brokerMode = BrokerMode.from(brokerMode);
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.relayUsername = relayUsername == null ? "" : relayUsername;
        this.relayPassword = relayPassword == null ? "" : relayPassword;
        this.relayVirtualHost = relayVirtualHost == null || relayVirtualHost.isBlank() ? "/" : relayVirtualHost;
        if (this.brokerMode == BrokerMode.RELAY) {
            if (this.relayUsername.isBlank() || this.relayPassword.isBlank()) {
                throw new IllegalStateException(
                        "STOMP broker relay requires app.realtime.relay.username and app.realtime.relay.password.");
            }
            if ("guest".equals(this.relayUsername) && "guest".equals(this.relayPassword)) {
                throw new IllegalStateException(
                        "guest/guest is not allowed for the STOMP broker relay. Set RABBITMQ_USERNAME and RABBITMQ_PASSWORD.");
            }
        }
    }

    public BrokerMode brokerMode() { return brokerMode; }
    public boolean relay() { return brokerMode == BrokerMode.RELAY; }
    public String relayHost() { return relayHost; }
    public int relayPort() { return relayPort; }
    public String relayUsername() { return relayUsername; }
    public String relayPassword() { return relayPassword; }
    public String relayVirtualHost() { return relayVirtualHost; }
}
