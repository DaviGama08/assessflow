package com.davigama.assessflow.locallive.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalPackageSettings {
    private final int maxQuestions;
    private final int maxOptionsPerQuestion;
    private final long maxPayloadBytes;

    public LocalPackageSettings(
            @Value("${app.local-package.max-questions:200}") int maxQuestions,
            @Value("${app.local-package.max-options-per-question:20}") int maxOptionsPerQuestion,
            @Value("${app.local-package.max-payload-bytes:2097152}") long maxPayloadBytes) {
        this.maxQuestions = maxQuestions;
        this.maxOptionsPerQuestion = maxOptionsPerQuestion;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public int maxQuestions() {
        return maxQuestions;
    }

    public int maxOptionsPerQuestion() {
        return maxOptionsPerQuestion;
    }

    public long maxPayloadBytes() {
        return maxPayloadBytes;
    }
}
