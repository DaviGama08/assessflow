package com.davigama.assessflow.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.davigama.assessflow.assessment.api.dto.CreateAssessmentRequest;
import com.davigama.assessflow.assessment.api.dto.UpdateAssessmentRequest;
import com.davigama.assessflow.assessment.application.AssessmentService;
import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentStatus;
import com.davigama.assessflow.assessment.infrastructure.AssessmentRepository;
import com.davigama.assessflow.shared.exception.AssessmentNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AssessmentServiceTest {
    @Mock AssessmentRepository repository;
    private AssessmentService service;
    private final Instant now = Instant.parse("2026-09-18T12:00:00Z");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AssessmentService(repository, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void createsDraftWithUuidAndUtcTimestamps() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(new CreateAssessmentRequest("  Quiz  ", "Description"));
        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Quiz");
        assertThat(response.status()).isEqualTo(AssessmentStatus.DRAFT);
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void findsUpdatesAndDeletesAssessment() {
        Assessment assessment = new Assessment("Before", null, now.minusSeconds(60));
        when(repository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        assertThat(service.get(assessment.getId()).title()).isEqualTo("Before");
        var updated = service.update(assessment.getId(), new UpdateAssessmentRequest("After", "Details"));
        assertThat(updated.title()).isEqualTo("After");
        assertThat(updated.updatedAt()).isEqualTo(now);
        service.delete(assessment.getId());
        verify(repository).delete(assessment);
    }

    @Test
    void missingAssessmentFailsAllLookupOperations() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(AssessmentNotFoundException.class);
        assertThatThrownBy(() -> service.update(id, new UpdateAssessmentRequest("Title", null))).isInstanceOf(AssessmentNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(AssessmentNotFoundException.class);
    }
}
