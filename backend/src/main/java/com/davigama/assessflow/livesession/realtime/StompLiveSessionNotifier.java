package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.domain.LiveEvent;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompLiveSessionNotifier implements LiveSessionNotifier {
    private final SimpMessagingTemplate messaging;

    public StompLiveSessionNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void publish(UUID sessionId, LiveEvent event) {
        messaging.convertAndSend("/topic/sessions/" + sessionId, event);
    }
}
