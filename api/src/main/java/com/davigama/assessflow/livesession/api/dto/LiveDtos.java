package com.davigama.assessflow.livesession.api.dto;

import com.davigama.assessflow.livesession.domain.LiveParticipant;
import com.davigama.assessflow.livesession.domain.LiveParticipantStatus;
import com.davigama.assessflow.livesession.domain.LiveSession;
import com.davigama.assessflow.livesession.domain.LiveSessionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LiveDtos {
    private LiveDtos() {}

    public record JoinRequest(@NotBlank @Size(max = 16) String code,
                              @NotBlank @Size(max = 200) String displayName) {}

    public record AnswerRequest(@NotEmpty List<UUID> optionIds) {}

    public record JoinResponse(UUID participantId, UUID sessionId, String sessionName, LiveSessionStatus status,
                               String joinCode, String participantToken) {}

    public record PreviewResponse(String sessionName, LiveSessionStatus status, boolean joinable) {}

    public record SessionResponse(UUID id, UUID organizationId, UUID assessmentId, String assessmentTitle,
                                  String joinCode, LiveSessionStatus status, Integer currentQuestionIndex,
                                  boolean questionOpen, Instant currentQuestionStartedAt, Instant startedAt,
                                  Instant finishedAt, Instant createdAt) {
        public static SessionResponse from(LiveSession session, String assessmentTitle) {
            return new SessionResponse(session.getId(), session.getOrganizationId(), session.getAssessmentId(),
                    assessmentTitle, session.getJoinCode(), session.getStatus(), session.getCurrentQuestionIndex(),
                    session.isQuestionOpen(), session.getCurrentQuestionStartedAt(), session.getStartedAt(),
                    session.getFinishedAt(), session.getCreatedAt());
        }
    }

    public record ParticipantResponse(UUID id, String displayName, LiveParticipantStatus status, Instant joinedAt) {
        public static ParticipantResponse from(LiveParticipant participant) {
            return new ParticipantResponse(participant.getId(), participant.getDisplayName(),
                    participant.getStatus(), participant.getJoinedAt());
        }
    }

    public record PublicOption(UUID id, String text) {}

    public record PublicQuestion(UUID questionId, String text, QuestionType type, int index, int total,
                                 List<PublicOption> options) {}

    public record OptionResult(UUID id, String text, boolean correct, long votes, double percent) {}

    public record PublicOptionStat(UUID id, String text, long votes, double percent) {}

    public record PublicQuestionResults(UUID questionId, String text, long answered, long participants,
                                        List<PublicOptionStat> options) {}

    public record QuestionResults(UUID questionId, String text, long answered, long participants,
                                  List<OptionResult> options) {}

    public record Score(int pointsEarned, int pointsPossible, double percentage) {}

    public record ParticipantState(UUID sessionId, String sessionName, LiveSessionStatus status, boolean questionOpen,
                                   boolean alreadyAnswered, PublicQuestion question, QuestionResults results,
                                   Score score, boolean showResults) {}
}
