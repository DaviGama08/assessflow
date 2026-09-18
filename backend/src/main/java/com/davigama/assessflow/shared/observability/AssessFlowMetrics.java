package com.davigama.assessflow.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AssessFlowMetrics {
    private final Counter joins;
    private final Counter answers;
    private final Counter sessionsStarted;
    private final Counter packageImports;
    private final Counter rateLimitAllowed;
    private final Counter rateLimitRejected;
    private final Counter rateLimitErrors;
    private final Counter realtimePublishFailures;

    public AssessFlowMetrics(MeterRegistry registry) {
        this.joins = Counter.builder("assessflow.live.joins")
                .description("Public live session joins")
                .register(registry);
        this.answers = Counter.builder("assessflow.live.answers")
                .description("Participant answers submitted")
                .register(registry);
        this.sessionsStarted = Counter.builder("assessflow.live.sessions.started")
                .description("Live sessions started")
                .register(registry);
        this.packageImports = Counter.builder("assessflow.package.imports")
                .description("Local event packages imported")
                .register(registry);
        this.rateLimitAllowed = Counter.builder("assessflow.rate_limit.decisions")
                .description("Public rate-limit decisions")
                .tag("outcome", "allowed")
                .register(registry);
        this.rateLimitRejected = Counter.builder("assessflow.rate_limit.decisions")
                .description("Public rate-limit decisions")
                .tag("outcome", "rejected")
                .register(registry);
        this.rateLimitErrors = Counter.builder("assessflow.rate_limit.decisions")
                .description("Public rate-limit decisions")
                .tag("outcome", "error")
                .register(registry);
        this.realtimePublishFailures = Counter.builder("assessflow.realtime.publish.failures")
                .description("STOMP publishes that failed after the domain transaction committed")
                .register(registry);
    }

    public void join() { joins.increment(); }
    public void answer() { answers.increment(); }
    public void sessionStarted() { sessionsStarted.increment(); }
    public void packageImport() { packageImports.increment(); }
    public void rateLimitAllowed() { rateLimitAllowed.increment(); }
    public void rateLimitRejected() { rateLimitRejected.increment(); }
    public void rateLimitError() { rateLimitErrors.increment(); }
    public void realtimePublishFailure() { realtimePublishFailures.increment(); }
}
