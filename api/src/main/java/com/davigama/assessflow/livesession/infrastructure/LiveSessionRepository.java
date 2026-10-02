package com.davigama.assessflow.livesession.infrastructure;

import com.davigama.assessflow.livesession.domain.LiveSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveSessionRepository extends JpaRepository<LiveSession, UUID> {
    Optional<LiveSession> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<LiveSession> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
