package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.identity.application.AuthService;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.livesession.application.ParticipantAuthService;
import com.davigama.assessflow.livesession.application.ParticipantPrincipal;
import com.davigama.assessflow.livesession.infrastructure.LiveSessionRepository;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final AuthService auth;
    private final ParticipantAuthService participants;
    private final LiveSessionRepository sessions;
    private final OrganizationAccess access;
    private final String[] allowedOrigins;

    public WebSocketConfig(AuthService auth, ParticipantAuthService participants, LiveSessionRepository sessions,
                           OrganizationAccess access,
                           @Value("${app.ws.allowed-origins:}") String wsOrigins,
                           @Value("${app.cors.allowed-origins}") String corsOrigins) {
        this.auth = auth;
        this.participants = participants;
        this.sessions = sessions;
        this.access = access;
        String raw = wsOrigins == null || wsOrigins.isBlank() ? corsOrigins : wsOrigins;
        this.allowedOrigins = java.util.Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");
                    if (header != null && header.startsWith("Bearer ")) {
                        String raw = header.substring(7);
                        auth.authenticate(raw).ifPresent(user -> accessor.setUser(
                                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of())));
                        if (accessor.getUser() == null) {
                            try {
                                ParticipantPrincipal participant = participants.authenticate(raw);
                                if (participant != null) {
                                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                                            participant, null, java.util.List.of()));
                                }
                            } catch (RuntimeException ex) {
                                throw new IllegalArgumentException("Unauthorized websocket");
                            }
                        }
                    }
                    if (accessor.getUser() == null) {
                        throw new IllegalArgumentException("Unauthorized websocket");
                    }
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscribe(accessor);
                }
                return message;
            }
        });
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        boolean hostTopic = destination != null && destination.startsWith("/topic/host/sessions/");
        boolean participantTopic = destination != null && destination.startsWith("/topic/sessions/");
        if (!hostTopic && !participantTopic) {
            throw new IllegalArgumentException("Invalid destination");
        }
        String prefix = hostTopic ? "/topic/host/sessions/" : "/topic/sessions/";
        UUID sessionId;
        try {
            sessionId = UUID.fromString(destination.substring(prefix.length()).split("/")[0]);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid destination");
        }
        var principal = accessor.getUser();
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) {
            throw new IllegalArgumentException("Forbidden destination");
        }
        if (token.getPrincipal() instanceof User user) {
            var session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Unknown session"));
            try {
                access.requireInstructor(session.getOrganizationId(), user.getId());
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Forbidden destination");
            }
            return;
        }
        if (token.getPrincipal() instanceof ParticipantPrincipal participant) {
            if (hostTopic || !participant.sessionId().equals(sessionId)) {
                throw new IllegalArgumentException("Forbidden destination");
            }
            return;
        }
        throw new IllegalArgumentException("Forbidden destination");
    }
}
