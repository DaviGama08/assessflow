package com.davigama.assessflow.assessment.application;

import com.davigama.assessflow.assessment.api.dto.AssessmentQuestionDtos.AddAssessmentQuestionRequest;
import com.davigama.assessflow.assessment.api.dto.AssessmentQuestionDtos.AssessmentQuestionResponse;
import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import com.davigama.assessflow.assessment.infrastructure.AssessmentQuestionRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.questionbank.application.QuestionService;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.exception.DomainException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentQuestionService {
    private final AssessmentQuestionRepository links;
    private final AssessmentService assessments;
    private final QuestionService questions;
    private final QuestionRepository questionRepository;
    private final OrganizationAccess access;

    public AssessmentQuestionService(AssessmentQuestionRepository links, AssessmentService assessments,
                                     QuestionService questions, QuestionRepository questionRepository,
                                     OrganizationAccess access) {
        this.links = links;
        this.assessments = assessments;
        this.questions = questions;
        this.questionRepository = questionRepository;
        this.access = access;
    }

    @Transactional
    public AssessmentQuestionResponse add(User actor, UUID organizationId, UUID assessmentId, UUID questionId,
                                          AddAssessmentQuestionRequest request) {
        assessments.requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.requireOwned(organizationId, assessmentId);
        assessment.requireNotArchived();
        Question question = questions.requireOwned(organizationId, questionId);
        if (!question.getOrganizationId().equals(assessment.getOrganizationId())) {
            throw new DomainException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                    "Question " + questionId + " was not found");
        }
        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new DomainException(HttpStatus.CONFLICT, "QUESTION_ARCHIVED",
                    "Archived questions cannot be added to an assessment.");
        }
        if (links.existsByAssessmentIdAndQuestionId(assessment.getId(), question.getId())) {
            throw duplicate();
        }
        int order = request.displayOrder() != null
                ? request.displayOrder()
                : (int) links.countByAssessmentId(assessment.getId()) + 1;
        AssessmentQuestion link = new AssessmentQuestion(assessment.getId(), question.getId(), request.points(), order);
        try {
            return AssessmentQuestionResponse.from(links.saveAndFlush(link), question);
        } catch (DataIntegrityViolationException ex) {
            throw duplicate();
        }
    }

    @Transactional(readOnly = true)
    public List<AssessmentQuestionResponse> list(User actor, UUID organizationId, UUID assessmentId) {
        assessments.requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.requireOwned(organizationId, assessmentId);
        List<AssessmentQuestion> items = links.findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId());
        if (items.isEmpty()) return List.of();
        Map<UUID, Question> byId = questionRepository
                .findByIdInAndOrganizationId(items.stream().map(AssessmentQuestion::getQuestionId).toList(),
                        organizationId)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        return items.stream()
                .map(item -> AssessmentQuestionResponse.from(item, byId.get(item.getQuestionId())))
                .toList();
    }

    @Transactional
    public AssessmentQuestionResponse updatePoints(User actor, UUID organizationId, UUID assessmentId,
                                                   UUID questionId, int points) {
        assessments.requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.requireOwned(organizationId, assessmentId);
        AssessmentQuestion link = findLink(assessment.getId(), questionId);
        Question question = questions.requireOwned(organizationId, questionId);
        link.setPoints(points);
        return AssessmentQuestionResponse.from(link, question);
    }

    @Transactional
    public void remove(User actor, UUID organizationId, UUID assessmentId, UUID questionId) {
        assessments.requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.requireOwned(organizationId, assessmentId);
        links.delete(findLink(assessment.getId(), questionId));
    }

    @Transactional
    public List<AssessmentQuestionResponse> reorder(User actor, UUID organizationId, UUID assessmentId,
                                                    List<UUID> questionIds) {
        assessments.requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.requireOwned(organizationId, assessmentId);
        List<AssessmentQuestion> items = links.findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId());
        Set<UUID> current = items.stream().map(AssessmentQuestion::getQuestionId).collect(Collectors.toSet());
        if (questionIds.size() != items.size() || !current.equals(new HashSet<>(questionIds))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION_ORDER",
                    "Reorder must include each question on the assessment exactly once.");
        }
        Map<UUID, AssessmentQuestion> byQuestion = items.stream()
                .collect(Collectors.toMap(AssessmentQuestion::getQuestionId, Function.identity()));
        for (int i = 0; i < questionIds.size(); i++) {
            byQuestion.get(questionIds.get(i)).setDisplayOrder(i + 1);
        }
        return list(actor, organizationId, assessmentId);
    }

    private AssessmentQuestion findLink(UUID assessmentId, UUID questionId) {
        return links.findByAssessmentIdAndQuestionId(assessmentId, questionId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ASSESSMENT_QUESTION_NOT_FOUND",
                        "Question is not part of this assessment."));
    }

    private DomainException duplicate() {
        return new DomainException(HttpStatus.CONFLICT, "QUESTION_ALREADY_ADDED",
                "This question is already part of the assessment.");
    }
}
