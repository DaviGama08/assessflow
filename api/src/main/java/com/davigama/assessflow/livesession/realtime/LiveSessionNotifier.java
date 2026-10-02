package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.domain.LiveEvent;
import java.util.UUID;

public interface LiveSessionNotifier {
    void toParticipants(UUID sessionId, LiveEvent event);

    void toHost(UUID sessionId, LiveEvent event);

    default void toBoth(UUID sessionId, LiveEvent event) {
        toParticipants(sessionId, event);
        toHost(sessionId, event);
    }
}
