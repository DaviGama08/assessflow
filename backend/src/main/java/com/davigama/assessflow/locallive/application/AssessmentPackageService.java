package com.davigama.assessflow.locallive.application;

import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import com.davigama.assessflow.assessment.domain.AssessmentStatus;
import com.davigama.assessflow.assessment.infrastructure.AssessmentQuestionRepository;
import com.davigama.assessflow.assessment.infrastructure.AssessmentRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.locallive.api.LocalLiveDtos.PackageImportResponse;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.organization.application.OrganizationService;
import com.davigama.assessflow.organization.domain.Organization;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.domain.AnswerOption;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.davigama.assessflow.questionbank.infrastructure.QuestionCategoryRepository;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.exception.DomainException;
import com.davigama.assessflow.shared.Slug;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Clock clock;

    public AssessmentPackageService(AssessmentRepository assessments, AssessmentQuestionRepository links,
                                    QuestionRepository questions, QuestionCategoryRepository categories,
                                    OrganizationRepository organizations, OrganizationService organizationService,
                                    OrganizationAccess access, Clock clock) {
        this.assessments = assessments;
        this.links = links;
        this.questions = questions;
        this.categories = categories;
        this.organizations = organizations;
        this.organizationService = organizationService;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportPackage(User actor, UUID organizationId, UUID assessmentId) {
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.findByIdAndOrganizationId(assessmentId, organizationId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ASSESSMENT_NOT_FOUND", "Assessment not found."));
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new DomainException(HttpStatus.CONFLICT, "ASSESSMENT_NOT_PUBLISHED",
                    "Only published assessments can be exported.");
        }
        Organization organization = organizations.findById(organizationId).orElseThrow();
        List<Map<String, Object>> items = new ArrayList<>();
        for (AssessmentQuestion link : links.findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId())) {
            Question question = questions.findWithOptionsByIdInAndOrganizationId(List.of(link.getQuestionId()), organizationId)
                    .stream().findFirst().orElseThrow();
            items.add(Map.of(
                    "sourceQuestionId", question.getId().toString(),
                    "text", question.getText(),
                    "type", question.getType().name(),
                    "difficulty", question.getDifficulty().name(),
                    "explanation", question.getExplanation() == null ? "" : question.getExplanation(),
                    "category", question.getCategory().getName(),
                    "points", link.getPoints(),
                    "displayOrder", link.getDisplayOrder(),
                    "options", question.getOptions().stream().map(option -> Map.of(
                            "text", option.getText(),
                            "correct", option.isCorrect(),
                            "displayOrder", option.getDisplayOrder())).toList()
            ));
        }
        Map<String, Object> assessmentBody = new LinkedHashMap<>();
        assessmentBody.put("sourceId", assessment.getId().toString());
        assessmentBody.put("title", assessment.getTitle());
        assessmentBody.put("description", assessment.getDescription() == null ? "" : assessment.getDescription());
        assessmentBody.put("timeLimitMinutes", assessment.getTimeLimitMinutes());
        assessmentBody.put("maxAttempts", assessment.getMaxAttempts());
        assessmentBody.put("passingScore", assessment.getPassingScore());
        assessmentBody.put("shuffleQuestions", assessment.isShuffleQuestions());
        assessmentBody.put("shuffleAnswers", assessment.isShuffleAnswers());
        assessmentBody.put("showResultsAfterCompletion", assessment.isShowResultsAfterCompletion());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemaVersion", 1);
        body.put("exportedAt", Instant.now().toString());
        body.put("sourceOrganizationId", organization.getId().toString());
        body.put("organizationName", organization.getName());
        body.put("assessment", assessmentBody);
        body.put("questions", items);
        body.put("checksumSha256", checksum(body));
        return body;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public PackageImportResponse importPackage(User actor, Map<String, Object> body) {
        if (body == null || !Integer.valueOf(1).equals(asInt(body.get("schemaVersion")))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PACKAGE_VERSION",
                    "This Local Event Package version is not supported.");
        }
        String expected = String.valueOf(body.get("checksumSha256"));
        if (expected == null || expected.isBlank() || !expected.equals(checksum(body))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "PACKAGE_CORRUPT",
                    "The package checksum does not match.");
        }
        Organization organization = organizations.findBySlug("local-events")
                .orElseGet(() -> organizationService.create(actor, "Local Events", "local-events"));
        access.requireInstructor(organization.getId(), actor.getId());
        Instant now = clock.instant();
        Map<String, Object> assessmentMap = (Map<String, Object>) body.get("assessment");
        Assessment assessment = new Assessment(organization.getId(), String.valueOf(assessmentMap.get("title")),
                String.valueOf(assessmentMap.getOrDefault("description", "")), now);
        assessment.configure(asInt(assessmentMap.get("timeLimitMinutes")), asInt(assessmentMap.get("maxAttempts")),
                asInt(assessmentMap.get("passingScore")), bool(assessmentMap.get("shuffleQuestions")),
                bool(assessmentMap.get("shuffleAnswers")), bool(assessmentMap.get("showResultsAfterCompletion")), now);
        assessments.save(assessment);
        int order = 0;
        for (Map<String, Object> item : (List<Map<String, Object>>) body.get("questions")) {
            String categoryName = String.valueOf(item.getOrDefault("category", "Imported"));
            String slug = Slug.normalize(categoryName);
            QuestionCategory category = categories.findByOrganizationIdAndSlug(organization.getId(), slug)
                    .orElseGet(() -> categories.save(new QuestionCategory(organization.getId(), categoryName, slug, now)));
            Question question = new Question(organization.getId(), category, String.valueOf(item.get("text")),
                    QuestionType.valueOf(String.valueOf(item.get("type"))),
                    QuestionDifficulty.valueOf(String.valueOf(item.getOrDefault("difficulty", "MEDIUM"))),
                    String.valueOf(item.getOrDefault("explanation", "")), QuestionStatus.ACTIVE, now);
            List<AnswerOption> options = new ArrayList<>();
            int optionOrder = 0;
            for (Map<String, Object> option : (List<Map<String, Object>>) item.get("options")) {
                options.add(new AnswerOption(question, String.valueOf(option.get("text")),
                        Boolean.TRUE.equals(option.get("correct")), optionOrder++));
            }
            question.replaceOptions(options);
            questions.save(question);
            int points = asInt(item.get("points")) == null ? 1 : asInt(item.get("points"));
            links.save(new AssessmentQuestion(assessment.getId(), question.getId(), points, order++));
        }
        assessment.publish(now);
        return new PackageImportResponse(organization.getId(), assessment.getId(), assessment.getTitle());
    }

    private Integer asInt(Object value) {
        if (value == null || "null".equals(String.valueOf(value))) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private Boolean bool(Object value) {
        if (value == null) return null;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private String checksum(Map<String, Object> body) {
        try {
            StringBuilder canonical = new StringBuilder();
            canonical.append(body.get("schemaVersion")).append('|');
            Map<String, Object> assessment = (Map<String, Object>) body.get("assessment");
            canonical.append(assessment.get("title")).append('|');
            for (Map<String, Object> item : (List<Map<String, Object>>) body.get("questions")) {
                canonical.append(item.get("text")).append('|').append(item.get("type")).append('|');
                canonical.append(item.get("points")).append('|');
                for (Map<String, Object> option : (List<Map<String, Object>>) item.get("options")) {
                    canonical.append(option.get("text")).append('=').append(option.get("correct")).append(';');
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
