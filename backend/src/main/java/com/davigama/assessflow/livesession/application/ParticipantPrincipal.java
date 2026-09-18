package com.davigama.assessflow.livesession.application;

import java.util.UUID;

public record ParticipantPrincipal(UUID participantId, UUID sessionId, UUID organizationId, String displayName) {}
