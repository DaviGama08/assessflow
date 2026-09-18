package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.application.LiveSessionService;
import com.davigama.assessflow.livesession.application.ParticipantPrincipal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class LiveSessionPresenceListener {
    private final LiveSessionService liveSessions;

    public LiveSessionPresenceListener(LiveSessionService liveSessions) {
        this.liveSessions = liveSessions;
    }

    @EventListener
    public void connected(SessionConnectedEvent event) {
        participant(event.getUser()).ifPresent(principal -> liveSessions.markConnected(principal.participantId()));
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        participant(event.getUser()).ifPresent(principal -> liveSessions.markDisconnected(principal.participantId()));
        if (event.getUser() == null) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            participant(accessor.getUser()).ifPresent(principal -> liveSessions.markDisconnected(principal.participantId()));
        }
    }

    private java.util.Optional<ParticipantPrincipal> participant(java.security.Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof ParticipantPrincipal participant) {
            return java.util.Optional.of(participant);
        }
        return java.util.Optional.empty();
    }
}
