package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.domain.LiveEvent;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AfterCommitLiveSessionNotifier implements LiveSessionNotifier {
    private final ApplicationEventPublisher publisher;

    public AfterCommitLiveSessionNotifier(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void toParticipants(UUID sessionId, LiveEvent event) {
        publisher.publishEvent(new LiveRealtimeMessage(sessionId, LiveRealtimeMessage.Audience.PARTICIPANTS, event));
    }

    @Override
    public void toHost(UUID sessionId, LiveEvent event) {
        publisher.publishEvent(new LiveRealtimeMessage(sessionId, LiveRealtimeMessage.Audience.HOST, event));
    }

    @Override
    public void toBoth(UUID sessionId, LiveEvent event) {
        publisher.publishEvent(new LiveRealtimeMessage(sessionId, LiveRealtimeMessage.Audience.BOTH, event));
    }
}
