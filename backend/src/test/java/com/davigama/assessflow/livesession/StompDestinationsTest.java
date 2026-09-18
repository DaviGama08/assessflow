package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.livesession.realtime.StompDestinations;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StompDestinationsTest {
    @Test
    void mapsPublicTopicsOntoRabbitRoutingKeysWithoutSlashes() {
        UUID session = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        assertThat(StompDestinations.forBroker(StompDestinations.participants(session)))
                .isEqualTo("/exchange/amq.topic/live.sessions." + session);
        assertThat(StompDestinations.forBroker(StompDestinations.host(session)))
                .isEqualTo("/exchange/amq.topic/live.host.sessions." + session);
        assertThat(StompDestinations.forBroker("/topic/other")).isEqualTo("/topic/other");
    }
}
