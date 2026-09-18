package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.domain.LiveEvent;
import java.util.UUID;

public interface LiveSessionNotifier {
    void publish(UUID sessionId, LiveEvent event);
}
