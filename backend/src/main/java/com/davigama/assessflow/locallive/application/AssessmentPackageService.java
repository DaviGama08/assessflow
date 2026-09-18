package com.davigama.assessflow.locallive.application;

import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import com.davigama.assessflow.assessment.domain.AssessmentStatus;
import com.davigama.assessflow.assessment.infrastructure.AssessmentQuestionRepository;
import com.davigama.assessflow.assessment.infrastructure.AssessmentRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.locallive.api.LocalLiveDtos.PackageImportResponse;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1.PackageAssessment;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1.PackageOption;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1.PackageQuestion;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.organization.application.OrganizationService;
import com.davigama.assessflow.organization.domain.Organization;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.domain.AnswerOption;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import com.davigama.assessflow.questionbank.domain.QuestionRules;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.infrastructure.QuestionCategoryRepository;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.Slug;
import com.davigama.assessflow.shared.exception.DomainException;
import com.davigama.assessflow.shared.observability.AssessFlowMetrics;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentPackageService {
    private final AssessmentRepository assessments;
    private final AssessmentQuestionRepository links;
    private final QuestionRepository questions;
    private final QuestionCategoryRepository categories;
    private final OrganizationRepository organizations;
    private final OrganizationService organizationService;
    private final OrganizationAccess access;
    private final LocalPackageSettings settings;
    private final AssessFlowMetrics metrics;
    private final Clock clock;

    public AssessmentPackageService(AssessmentRepository assessments, AssessmentQuestionRepository links,
                                    QuestionRepository questions, QuestionCategoryRepository categories,
                                    OrganizationRepository organizations, OrganizationService organizationService,
                                    OrganizationAccess access, LocalPackageSettings settings,
                                    AssessFlowMetrics metrics, Clock clock) {
        this.assessments = assessments;
        this.links = links;
        this.questions = questions;
        this.categories = categories;
        this.organizations = organizations;
        this.organizationService = organizationService;
        this.access = access;
        this.settings = settings;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssessmentPackageV1 exportPackage(User actor, UUID organizationId, UUID assessmentId) {
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.findByIdAndOrganizationId(assessmentId, organizationId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ASSESSMENT_NOT_FOUND", "Assessment not found."));
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new DomainException(HttpStatus.CONFLICT, "ASSESSMENT_NOT_PUBLISHED",
                    "Only published assessments can be exported.");
        }
        Organization organization = organizations.findById(organizationId).orElseThrow();
        List<PackageQuestion> items = new ArrayList<>();
        for (AssessmentQuestion link : links.findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId())) {
            Question question = questions.findWithOptionsByIdInAndOrganizationId(List.of(link.getQuestionId()), organizationId)
                    .stream().findFirst().orElseThrow();
            List<PackageOption> options = question.getOptions().stream()
                    .sorted(Comparator.comparingInt(AnswerOption::getDisplayOrder).thenComparing(AnswerOption::getId))
                    .map(option -> new PackageOption(option.getText(), option.isCorrect(), option.getDisplayOrder()))
                    .toList();
            items.add(new PackageQuestion(
                    question.getId(),
                    question.getText(),
                    question.getType(),
                    question.getDifficulty(),
                    question.getExplanation() == null ? "" : question.getExplanation(),
                    question.getCategory().getName(),
                    link.getPoints(),
                    link.getDisplayOrder(),
                    options));
        }
        Instant exportedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        PackageAssessment assessmentBody = new PackageAssessment(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getDescription() == null ? "" : assessment.getDescription(),
                assessment.getTimeLimitMinutes(),
                assessment.getMaxAttempts(),
                assessment.getPassingScore(),
                assessment.isShuffleQuestions(),
                assessment.isShuffleAnswers(),
                assessment.isShowResultsAfterCompletion());
        AssessmentPackageV1 pack = new AssessmentPackageV1(
                AssessmentPackageV1.CURRENT_SCHEMA_VERSION,
                exportedAt,
                organization.getId(),
                organization.getName(),
                assessmentBody,
                List.copyOf(items),
                "pending");
        return new AssessmentPackageV1(
                pack.schemaVersion(),
                pack.exportedAt(),
                pack.sourceOrganizationId(),
                pack.organizationName(),
                pack.assessment(),
                pack.questions(),
                PackageChecksum.sha256(pack.content()));
    }

    @Transactional
    public PackageImportResponse importPackage(User actor, AssessmentPackageV1 pack) {
        if (pack == null) {
            throw invalidPackage("The local event package is invalid.");
        }
        if (pack.schemaVersion() == null || pack.schemaVersion() != AssessmentPackageV1.CURRENT_SCHEMA_VERSION) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PACKAGE_VERSION",
                    "This Local Event Package version is not supported.");
        }
        if (!PackageChecksum.matches(pack)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "PACKAGE_CORRUPT",
                    "The package checksum does not match.");
        }
        validateLimits(pack);
        Organization organization = organizations.findBySlug("local-events")
                .orElseGet(() -> organizationService.create(actor, "Local Events", "local-events"));
        access.requireInstructor(organization.getId(), actor.getId());
        Instant now = clock.instant();
        PackageAssessment assessmentBody = pack.assessment();
        Assessment assessment = new Assessment(organization.getId(), assessmentBody.title(),
                assessmentBody.description() == null ? "" : assessmentBody.description(), now);
        assessment.configure(assessmentBody.timeLimitMinutes(), assessmentBody.maxAttempts(),
                assessmentBody.passingScore(), assessmentBody.shuffleQuestions(),
                assessmentBody.shuffleAnswers(), assessmentBody.showResultsAfterCompletion(), now);
        assessment = assessments.save(assessment);
        int fallbackOrder = 0;
        for (PackageQuestion item : pack.questions()) {
            String categoryName = item.category() == null || item.category().isBlank() ? "Imported" : item.category();
            String slug = Slug.normalize(categoryName);
            QuestionCategory category = categories.findByOrganizationIdAndSlug(organization.getId(), slug)
                    .orElseGet(() -> categories.save(new QuestionCategory(organization.getId(), categoryName, slug, now)));
            Question question = new Question(organization.getId(), category, item.text(), item.type(), item.difficulty(),
                    item.explanation() == null ? "" : item.explanation(), QuestionStatus.ACTIVE, now);
            List<AnswerOption> options = item.options().stream()
                    .sorted(Comparator.comparingInt(PackageOption::displayOrder))
                    .map(option -> new AnswerOption(question, option.text(), option.correct(), option.displayOrder()))
                    .toList();
            try {
                QuestionRules.requireValid(item.type(), options);
                question.replaceOptions(new ArrayList<>(options));
            } catch (DomainException ex) {
                throw invalidPackage(ex.getMessage());
            }
            Question persisted = questions.save(question);
            int order = item.displayOrder() >= 0 ? item.displayOrder() : fallbackOrder;
            links.save(new AssessmentQuestion(assessment.getId(), persisted.getId(), item.points(), order));
            fallbackOrder++;
        }
        assessment.publish(now);
        assessments.save(assessment);
        metrics.packageImport();
        return new PackageImportResponse(organization.getId(), assessment.getId(), assessment.getTitle());
    }

    private void validateLimits(AssessmentPackageV1 pack) {
        if (pack.questions().size() > settings.maxQuestions()) {
            throw invalidPackage("The local event package has too many questions.");
        }
        for (PackageQuestion question : pack.questions()) {
            if (question.options().size() > settings.maxOptionsPerQuestion()) {
                throw invalidPackage("A question in the local event package has too many options.");
            }
        }
    }

    private static DomainException invalidPackage(String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, "INVALID_LOCAL_PACKAGE", message);
    }
}
