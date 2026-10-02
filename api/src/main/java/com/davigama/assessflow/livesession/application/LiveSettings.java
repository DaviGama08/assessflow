package com.davigama.assessflow.livesession.application;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiveSettings {
    private final Duration participantTokenTtl;

    public LiveSettings(@Value("${app.live.participant-token-ttl:12h}") Duration participantTokenTtl) {
        this.participantTokenTtl = participantTokenTtl;
    }

    public Duration participantTokenTtl() {
        return participantTokenTtl;
    }
}
