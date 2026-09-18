package com.davigama.assessflow.livesession.infrastructure;

import com.davigama.assessflow.livesession.domain.LiveParticipant;
import com.davigama.assessflow.livesession.domain.LiveParticipantStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveParticipantRepository extends JpaRepository<LiveParticipant, UUID> {
    Optional<LiveParticipant> findByTokenHash(String tokenHash);

    Optional<LiveParticipant> findByIdAndLiveSessionId(UUID id, UUID liveSessionId);

    List<LiveParticipant> findByLiveSessionIdOrderByJoinedAtAsc(UUID liveSessionId);

    long countByLiveSessionIdAndStatusNot(UUID liveSessionId, LiveParticipantStatus status);
}
