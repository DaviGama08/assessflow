package com.davigama.assessflow.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.davigama.assessflow.assessment.api.dto.CreateAssessmentRequest;
import com.davigama.assessflow.assessment.api.dto.UpdateAssessmentRequest;
import com.davigama.assessflow.assessment.application.AssessmentService;
import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentStatus;
import com.davigama.assessflow.assessment.infrastructure.AssessmentQuestionRepository;
import com.davigama.assessflow.assessment.infrastructure.AssessmentRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.exception.AssessmentNotFoundException;
import com.davigama.assessflow.shared.exception.DomainException;
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
    @Mock AssessmentQuestionRepository links;
    @Mock QuestionRepository questions;
    @Mock OrganizationRepository organizations;
    @Mock OrganizationAccess access;
    private AssessmentService service;
    private final Instant now = Instant.parse("2026-09-18T12:00:00Z");
    private User actor;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AssessmentService(repository, links, questions, organizations, access,
                Clock.fixed(now, ZoneOffset.UTC));
        actor = new User("owner@example.com", "hash", "Owner", now);
        organizationId = UUID.randomUUID();
        when(organizations.existsById(organizationId)).thenReturn(true);
    }

    @Test
    void createsDraftWithUuidUtcTimestampsAndDefaults() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(actor, organizationId,
                new CreateAssessmentRequest("  Quiz  ", "Description", null, null, null, null, null, null));
        assertThat(response.id()).isNotNull();
        assertThat(response.organizationId()).isEqualTo(organizationId);
        assertThat(response.title()).isEqualTo("Quiz");
        assertThat(response.status()).isEqualTo(AssessmentStatus.DRAFT);
        assertThat(response.maxAttempts()).isEqualTo(1);
        assertThat(response.shuffleQuestions()).isFalse();
        assertThat(response.shuffleAnswers()).isFalse();
        assertThat(response.showResultsAfterCompletion()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
        verify(access).requireInstructor(organizationId, actor.getId());
    }

    @Test
    void findsUpdatesAndDeletesAssessmentWithinOrganization() {
        Assessment assessment = new Assessment(organizationId, "Before", null, now.minusSeconds(60));
        when(repository.findByIdAndOrganizationId(assessment.getId(), organizationId))
                .thenReturn(Optional.of(assessment));
        assertThat(service.get(actor, organizationId, assessment.getId()).title()).isEqualTo("Before");
        var updated = service.update(actor, organizationId, assessment.getId(),
                new UpdateAssessmentRequest("After", "Details", 30, 2, 70, true, true, false));
        assertThat(updated.title()).isEqualTo("After");
        assertThat(updated.timeLimitMinutes()).isEqualTo(30);
        assertThat(updated.maxAttempts()).isEqualTo(2);
        assertThat(updated.passingScore()).isEqualTo(70);
        assertThat(updated.shuffleQuestions()).isTrue();
        assertThat(updated.showResultsAfterCompletion()).isFalse();
        service.delete(actor, organizationId, assessment.getId());
        verify(repository).delete(assessment);
    }

    @Test
    void missingAssessmentFailsAllLookupOperations() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndOrganizationId(id, organizationId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(actor, organizationId, id))
                .isInstanceOf(AssessmentNotFoundException.class);
        assertThatThrownBy(() -> service.update(actor, organizationId, id,
                new UpdateAssessmentRequest("Title", null, null, null, null, null, null, null)))
                .isInstanceOf(AssessmentNotFoundException.class);
        assertThatThrownBy(() -> service.delete(actor, organizationId, id))
                .isInstanceOf(AssessmentNotFoundException.class);
    }

    @Test
    void rejectsInvalidConfiguration() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThatThrownBy(() -> service.create(actor, organizationId,
                new CreateAssessmentRequest("Quiz", null, 0, null, null, null, null, null)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Time limit");
        Assessment assessment = new Assessment(organizationId, "Quiz", null, now);
        when(repository.findByIdAndOrganizationId(eq(assessment.getId()), eq(organizationId)))
                .thenReturn(Optional.of(assessment));
        assertThatThrownBy(() -> service.update(actor, organizationId, assessment.getId(),
                new UpdateAssessmentRequest("Quiz", null, null, 0, null, null, null, null)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> service.update(actor, organizationId, assessment.getId(),
                new UpdateAssessmentRequest("Quiz", null, null, 1, 101, null, null, null)))
                .isInstanceOf(DomainException.class);
    }
}
