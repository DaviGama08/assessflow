package com.davigama.assessflow.livesession.infrastructure;

import com.davigama.assessflow.livesession.domain.LiveSessionQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveSessionQuestionRepository extends JpaRepository<LiveSessionQuestion, UUID> {
    @EntityGraph(attributePaths = "options")
    List<LiveSessionQuestion> findByLiveSessionIdOrderByDisplayOrderAsc(UUID liveSessionId);

    @EntityGraph(attributePaths = "options")
    Optional<LiveSessionQuestion> findByIdAndLiveSessionId(UUID id, UUID liveSessionId);

    long countByLiveSessionId(UUID liveSessionId);
}
