package com.davigama.assessflow.livesession.application;

import com.davigama.assessflow.livesession.domain.LiveParticipant;
import com.davigama.assessflow.livesession.infrastructure.LiveParticipantRepository;
import com.davigama.assessflow.livesession.infrastructure.LiveSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class ParticipantAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final LiveParticipantRepository participants;
    private final LiveSessionRepository sessions;

    public ParticipantAuthService(LiveParticipantRepository participants, LiveSessionRepository sessions) {
        this.participants = participants;
        this.sessions = sessions;
    }

    public ParticipantPrincipal authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        LiveParticipant participant = participants.findByTokenHash(hash(rawToken)).orElse(null);
        if (participant == null) return null;
        return sessions.findById(participant.getLiveSessionId())
                .map(session -> new ParticipantPrincipal(participant.getId(), session.getId(),
                        session.getOrganizationId(), participant.getDisplayName()))
                .orElse(null);
    }

    public String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
