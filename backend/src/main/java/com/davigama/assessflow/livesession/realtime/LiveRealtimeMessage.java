package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.domain.LiveEvent;
import java.util.UUID;

public record LiveRealtimeMessage(UUID sessionId, Audience audience, LiveEvent event) {
    public enum Audience { HOST, PARTICIPANTS, BOTH }
}
