package com.davigama.assessflow.questionbank.application;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.organization.application.OrganizationException;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.CreateQuestionRequest;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.OptionInput;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.QuestionResponse;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.UpdateQuestionRequest;
import com.davigama.assessflow.questionbank.domain.AnswerOption;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.exception.DomainException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {
    private final QuestionRepository questions;
    private final QuestionCategoryService categories;
    private final OrganizationRepository organizations;
    private final OrganizationAccess access;
    private final Clock clock;

    public QuestionService(QuestionRepository questions, QuestionCategoryService categories,
                           OrganizationRepository organizations, OrganizationAccess access, Clock clock) {
        this.questions = questions;
        this.categories = categories;
        this.organizations = organizations;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public QuestionResponse create(User actor, UUID organizationId, CreateQuestionRequest request) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        QuestionStatus status = request.status() == null ? QuestionStatus.DRAFT : request.status();
        if (status == QuestionStatus.ARCHIVED) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION_STATUS",
                    "New questions cannot be created as archived.");
        }
        QuestionCategory category = categories.requireOwned(organizationId, request.categoryId());
        Question question = new Question(organizationId, category, request.text(), request.type(),
                request.difficulty(), request.explanation(), status, clock.instant());
        question.replaceOptions(toOptions(question, request.options()));
        return QuestionResponse.from(questions.save(question), true);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> list(User actor, UUID organizationId, int page, int size, String search,
                                       QuestionType type, QuestionDifficulty difficulty, QuestionStatus status,
                                       UUID categoryId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        String term = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return questions.search(organizationId, type, difficulty, status, categoryId, term, pageable)
                .map(question -> QuestionResponse.from(question, false));
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(User actor, UUID organizationId, UUID questionId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        return QuestionResponse.from(findDetailed(organizationId, questionId), true);
    }

    @Transactional
    public QuestionResponse update(User actor, UUID organizationId, UUID questionId, UpdateQuestionRequest request) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Question question = findDetailed(organizationId, questionId);
        QuestionCategory category = categories.requireOwned(organizationId, request.categoryId());
        question.update(category, request.text(), request.type(), request.difficulty(), request.explanation(),
                request.status(), clock.instant());
        question.replaceOptions(toOptions(question, request.options()));
        return QuestionResponse.from(question, true);
    }

    @Transactional
    public void delete(User actor, UUID organizationId, UUID questionId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Question question = questions.findByIdAndOrganizationId(questionId, organizationId)
                .orElseThrow(() -> notFound(questionId));
        question.archive(clock.instant());
    }

    public Question requireOwned(UUID organizationId, UUID questionId) {
        return questions.findByIdAndOrganizationId(questionId, organizationId)
                .orElseThrow(() -> notFound(questionId));
    }

    private Question findDetailed(UUID organizationId, UUID questionId) {
        return questions.findDetailedByIdAndOrganizationId(questionId, organizationId)
                .orElseThrow(() -> notFound(questionId));
    }

    private List<AnswerOption> toOptions(Question question, List<OptionInput> inputs) {
        List<AnswerOption> options = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            OptionInput input = inputs.get(i);
            int order = input.displayOrder() != null ? input.displayOrder() : i;
            options.add(new AnswerOption(question, input.text(), input.correct(), order));
        }
        return options;
    }

    private void requireOrganization(UUID organizationId) {
        if (!organizations.existsById(organizationId)) {
            throw new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND",
                    "Organization not found.");
        }
    }

    private DomainException notFound(UUID questionId) {
        return new DomainException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                "Question " + questionId + " was not found");
    }
}
