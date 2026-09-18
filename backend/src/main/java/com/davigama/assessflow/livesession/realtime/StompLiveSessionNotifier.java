package com.davigama.assessflow.livesession.realtime;

import com.davigama.assessflow.livesession.domain.LiveEvent;
import com.davigama.assessflow.shared.config.RealtimeSettings;
import com.davigama.assessflow.shared.observability.AssessFlowMetrics;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StompLiveSessionNotifier {
    private static final Logger log = LoggerFactory.getLogger(StompLiveSessionNotifier.class);
    private final SimpMessagingTemplate messaging;
    private final AssessFlowMetrics metrics;
    private final RealtimeSettings realtime;

    public StompLiveSessionNotifier(SimpMessagingTemplate messaging, AssessFlowMetrics metrics,
                                    RealtimeSettings realtime) {
        this.messaging = messaging;
        this.metrics = metrics;
        this.realtime = realtime;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommitted(LiveRealtimeMessage message) {
        try {
            switch (message.audience()) {
                case HOST -> toHost(message.sessionId(), message.event());
                case PARTICIPANTS -> toParticipants(message.sessionId(), message.event());
                case BOTH -> {
                    toParticipants(message.sessionId(), message.event());
                    toHost(message.sessionId(), message.event());
                }
            }
        } catch (RuntimeException ex) {
            metrics.realtimePublishFailure();
            log.warn("Live realtime publish failed after commit session={} type={}",
                    message.sessionId(), message.event().type(), ex);
        }
    }

    public void toParticipants(UUID sessionId, LiveEvent event) {
        messaging.convertAndSend(brokerDestination(StompDestinations.participants(sessionId)), event);
    }

    public void toHost(UUID sessionId, LiveEvent event) {
        messaging.convertAndSend(brokerDestination(StompDestinations.host(sessionId)), event);
    }

    private String brokerDestination(String publicDestination) {
        return realtime.relay() ? StompDestinations.forBroker(publicDestination) : publicDestination;
    }
}
