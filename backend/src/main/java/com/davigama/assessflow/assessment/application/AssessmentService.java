package com.davigama.assessflow.assessment.application;

import com.davigama.assessflow.assessment.api.dto.AssessmentResponse;
import com.davigama.assessflow.assessment.api.dto.CreateAssessmentRequest;
import com.davigama.assessflow.assessment.api.dto.UpdateAssessmentRequest;
import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import com.davigama.assessflow.assessment.infrastructure.AssessmentQuestionRepository;
import com.davigama.assessflow.assessment.infrastructure.AssessmentRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.organization.application.OrganizationException;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.exception.AssessmentNotFoundException;
import com.davigama.assessflow.shared.exception.DomainException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {
    private final AssessmentRepository repository;
    private final AssessmentQuestionRepository links;
    private final QuestionRepository questions;
    private final OrganizationRepository organizations;
    private final OrganizationAccess access;
    private final Clock clock;

    public AssessmentService(AssessmentRepository repository, AssessmentQuestionRepository links,
                             QuestionRepository questions, OrganizationRepository organizations,
                             OrganizationAccess access, Clock clock) {
        this.repository = repository;
        this.links = links;
        this.questions = questions;
        this.organizations = organizations;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public AssessmentResponse create(User actor, UUID organizationId, CreateAssessmentRequest request) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = new Assessment(organizationId, request.title(), request.description(), clock.instant());
        assessment.configure(request.timeLimitMinutes(), request.maxAttempts(), request.passingScore(),
                request.shuffleQuestions(), request.shuffleAnswers(), request.showResultsAfterCompletion(),
                clock.instant());
        return AssessmentResponse.from(repository.save(assessment));
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponse> list(User actor, UUID organizationId, int page, int size) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return repository.findByOrganizationId(organizationId, pageable).map(AssessmentResponse::from);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(User actor, UUID organizationId, UUID assessmentId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        return AssessmentResponse.from(find(organizationId, assessmentId));
    }

    @Transactional
    public AssessmentResponse update(User actor, UUID organizationId, UUID assessmentId,
                                     UpdateAssessmentRequest request) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = find(organizationId, assessmentId);
        assessment.update(request.title(), request.description(), clock.instant());
        assessment.configure(request.timeLimitMinutes(), request.maxAttempts(), request.passingScore(),
                request.shuffleQuestions(), request.shuffleAnswers(), request.showResultsAfterCompletion(),
                clock.instant());
        return AssessmentResponse.from(assessment);
    }

    @Transactional
    public AssessmentResponse publish(User actor, UUID organizationId, UUID assessmentId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = find(organizationId, assessmentId);
        List<AssessmentQuestion> items = links.findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId());
        if (items.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "ASSESSMENT_HAS_NO_QUESTIONS",
                    "An assessment needs at least one question before it can be published.");
        }
        List<Question> bank = questions.findByIdInAndOrganizationId(
                items.stream().map(AssessmentQuestion::getQuestionId).toList(), organizationId);
        if (bank.size() != items.size()
                || bank.stream().anyMatch(question -> question.getStatus() != QuestionStatus.ACTIVE)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "ASSESSMENT_HAS_INVALID_QUESTIONS",
                    "Every linked question must be active before the assessment can be published.");
        }
        assessment.publish(clock.instant());
        return AssessmentResponse.from(assessment);
    }

    @Transactional
    public AssessmentResponse archive(User actor, UUID organizationId, UUID assessmentId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = find(organizationId, assessmentId);
        assessment.archive(clock.instant());
        return AssessmentResponse.from(assessment);
    }

    @Transactional
    public void delete(User actor, UUID organizationId, UUID assessmentId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = find(organizationId, assessmentId);
        assessment.requireNotArchived();
        repository.delete(assessment);
    }

    Assessment requireOwned(UUID organizationId, UUID assessmentId) {
        return find(organizationId, assessmentId);
    }

    void requireOrganization(UUID organizationId) {
        if (!organizations.existsById(organizationId)) {
            throw new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND",
                    "Organization not found.");
        }
    }

    private Assessment find(UUID organizationId, UUID assessmentId) {
        return repository.findByIdAndOrganizationId(assessmentId, organizationId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }
}
