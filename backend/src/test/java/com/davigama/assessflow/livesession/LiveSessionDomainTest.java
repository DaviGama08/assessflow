package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.davigama.assessflow.livesession.domain.JoinCodes;
import com.davigama.assessflow.livesession.domain.LiveSession;
import com.davigama.assessflow.livesession.domain.LiveSessionStatus;
import com.davigama.assessflow.shared.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LiveSessionDomainTest {
    private final Instant now = Instant.parse("2026-09-18T12:00:00Z");

    @Test
    void startsEndsAndRejectsInvalidTransitions() {
        LiveSession session = new LiveSession(UUID.randomUUID(), UUID.randomUUID(), JoinCodes.generate(),
                UUID.randomUUID(), now);
        assertThat(session.getStatus()).isEqualTo(LiveSessionStatus.WAITING);
        session.start(now);
        assertThat(session.getStatus()).isEqualTo(LiveSessionStatus.ACTIVE);
        assertThat(session.getCurrentQuestionIndex()).isZero();
        assertThat(session.isQuestionOpen()).isTrue();
        assertThatThrownBy(() -> session.start(now)).isInstanceOf(DomainException.class);
        session.endQuestion(now);
        assertThat(session.isQuestionOpen()).isFalse();
        session.nextQuestion(1, now);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
        session.endQuestion(now);
        session.finish(now);
        assertThat(session.getStatus()).isEqualTo(LiveSessionStatus.FINISHED);
        assertThatThrownBy(() -> session.finish(now)).isInstanceOf(DomainException.class);
        assertThat(session.joinable()).isFalse();
    }

    @Test
    void waitingSessionCanBeCancelledAndCannotStartAfterwards() {
        LiveSession session = new LiveSession(UUID.randomUUID(), UUID.randomUUID(), JoinCodes.generate(),
                UUID.randomUUID(), now);
        session.cancel(now);
        assertThat(session.getStatus()).isEqualTo(LiveSessionStatus.CANCELLED);
        assertThat(session.joinable()).isFalse();
        assertThatThrownBy(() -> session.start(now)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> session.cancel(now)).isInstanceOf(DomainException.class);
    }
}
