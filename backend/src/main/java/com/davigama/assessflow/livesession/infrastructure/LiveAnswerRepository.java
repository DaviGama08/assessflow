package com.davigama.assessflow.livesession.infrastructure;

import com.davigama.assessflow.livesession.domain.LiveAnswer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveAnswerRepository extends JpaRepository<LiveAnswer, UUID> {
    boolean existsByLiveSessionIdAndLiveSessionQuestionIdAndParticipantId(
            UUID liveSessionId, UUID liveSessionQuestionId, UUID participantId);

    Optional<LiveAnswer> findByLiveSessionIdAndLiveSessionQuestionIdAndParticipantId(
            UUID liveSessionId, UUID liveSessionQuestionId, UUID participantId);

    @EntityGraph(attributePaths = "optionIds")
    List<LiveAnswer> findByLiveSessionIdAndLiveSessionQuestionId(UUID liveSessionId, UUID liveSessionQuestionId);

    @EntityGraph(attributePaths = "optionIds")
    List<LiveAnswer> findByLiveSessionIdAndParticipantId(UUID liveSessionId, UUID participantId);

    long countByLiveSessionIdAndLiveSessionQuestionId(UUID liveSessionId, UUID liveSessionQuestionId);
}
