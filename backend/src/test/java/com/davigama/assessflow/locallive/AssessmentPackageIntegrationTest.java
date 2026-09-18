package com.davigama.assessflow.locallive;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.local-live.enabled=true")
@Testcontainers
@ActiveProfiles("test")
class AssessmentPackageIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    private ApiSupport api;

    @BeforeEach
    void setUp() {
        api = new ApiSupport(port);
    }

    @Test
    void exportsPublishedAssessmentAndRejectsDraft() throws Exception {
        Fixture fixture = published("pkg-owner@example.com", "Package Org", "package-org");
        assertThat(api.send("GET", fixture.packagePath, null, fixture.owner).statusCode()).isEqualTo(200);
        String draftId = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + fixture.organizationId + "/assessments",
                "{\"title\":\"Draft Quiz\"}", fixture.owner).body(), "id");
        var draft = api.send("GET",
                "/api/v1/organizations/" + fixture.organizationId + "/assessments/" + draftId + "/package",
                null, fixture.owner);
        assertThat(draft.statusCode()).isEqualTo(409);
        assertThat(draft.body()).contains("ASSESSMENT_NOT_PUBLISHED");
    }

    @Test
    void roundTripsPackagePreservingOrderPointsCorrectnessAndSettings() throws Exception {
        Fixture fixture = published("pkg-round@example.com", "Round Org", "round-org");
        var exported = api.send("GET", fixture.packagePath, null, fixture.owner);
        assertThat(exported.statusCode()).isEqualTo(200);
        assertThat(exported.body()).contains("\"schemaVersion\":1", "\"passingScore\":70", "\"maxAttempts\":2",
                "\"shuffleQuestions\":true", "\"shuffleAnswers\":true", "\"difficulty\":\"HARD\"",
                "\"points\":3", "First question", "Second question");
        var imported = api.send("POST", "/api/v1/local-live/packages", exported.body(), fixture.owner);
        assertThat(imported.statusCode()).isEqualTo(200);
        String importedOrg = ApiSupport.field(imported.body(), "organizationId");
        String importedAssessment = ApiSupport.field(imported.body(), "assessmentId");
        var assessment = api.send("GET",
                "/api/v1/organizations/" + importedOrg + "/assessments/" + importedAssessment, null, fixture.owner);
        assertThat(assessment.body()).contains("\"passingScore\":70", "\"maxAttempts\":2", "\"timeLimitMinutes\":45",
                "\"shuffleQuestions\":true", "\"shuffleAnswers\":true", "\"showResultsAfterCompletion\":false",
                "\"status\":\"PUBLISHED\"");
        var links = api.send("GET",
                "/api/v1/organizations/" + importedOrg + "/assessments/" + importedAssessment + "/questions",
                null, fixture.owner);
        assertThat(links.body().indexOf("First question")).isLessThan(links.body().indexOf("Second question"));
        assertThat(links.body()).contains("\"points\":1", "\"points\":3");
        String importedQuestionId = ApiSupport.field(links.body(), "questionId");
        var question = api.send("GET",
                "/api/v1/organizations/" + importedOrg + "/questions/" + importedQuestionId, null, fixture.owner);
        assertThat(question.body()).contains("\"correct\":true", "\"correct\":false", "Because");
    }

    @Test
    void rejectsCorruptUnsupportedMalformedAndUnauthorizedPackages() throws Exception {
        Fixture fixture = published("pkg-guard@example.com", "Guard Org", "guard-org");
        String outsider = api.register("pkg-outsider@example.com");
        String otherOrg = api.createOrganization(outsider, "Other Pkg", "other-pkg");
        String participant = api.register("pkg-participant@example.com");
        api.addMember(fixture.owner, fixture.organizationId, "pkg-participant@example.com", "PARTICIPANT");
        String pack = api.send("GET", fixture.packagePath, null, fixture.owner).body();

        assertThat(api.send("GET", fixture.packagePath, null, outsider).statusCode()).isEqualTo(403);
        assertThat(api.send("GET",
                "/api/v1/organizations/" + otherOrg + "/assessments/" + fixture.assessmentId + "/package",
                null, outsider).statusCode()).isEqualTo(404);
        assertThat(api.send("GET", fixture.packagePath, null, participant).statusCode()).isEqualTo(403);
        assertThat(api.send("POST", "/api/v1/local-live/packages", pack, null).statusCode()).isEqualTo(401);

        var unsupported = api.send("POST", "/api/v1/local-live/packages",
                pack.replace("\"schemaVersion\":1", "\"schemaVersion\":99"), fixture.owner);
        assertThat(unsupported.statusCode()).isEqualTo(400);
        assertThat(unsupported.body()).contains("UNSUPPORTED_PACKAGE_VERSION");

        var missing = api.send("POST", "/api/v1/local-live/packages",
                pack.replaceFirst("\"title\":\"[^\"]+\",", ""), fixture.owner);
        assertThat(missing.statusCode()).isEqualTo(400);
        assertThat(missing.body()).contains("INVALID_LOCAL_PACKAGE");

        var malformed = api.send("POST", "/api/v1/local-live/packages",
                pack.replace("\"SINGLE_CHOICE\"", "\"NOT_A_TYPE\""), fixture.owner);
        assertThat(malformed.statusCode()).isEqualTo(400);
        assertThat(malformed.body()).contains("INVALID_LOCAL_PACKAGE");

        var checksum = api.send("POST", "/api/v1/local-live/packages",
                pack.replaceAll("\"checksumSha256\":\"[^\"]+\"",
                        "\"checksumSha256\":\"" + "a".repeat(64) + "\""), fixture.owner);
        assertThat(checksum.statusCode()).isEqualTo(400);
        assertThat(checksum.body()).contains("PACKAGE_CORRUPT");

        assertThat(api.send("POST", "/api/v1/local-live/packages",
                pack.replace("\"passingScore\":70", "\"passingScore\":10"), fixture.owner).body())
                .contains("PACKAGE_CORRUPT");
        assertThat(api.send("POST", "/api/v1/local-live/packages",
                pack.replace("\"HARD\"", "\"EASY\""), fixture.owner).body())
                .contains("PACKAGE_CORRUPT");
        assertThat(api.send("POST", "/api/v1/local-live/packages",
                pack.replace("\"Yes\"", "\"Tampered\""), fixture.owner).body())
                .contains("PACKAGE_CORRUPT");
    }

    private Fixture published(String email, String orgName, String slug) throws Exception {
        String owner = api.register(email);
        String organizationId = api.createOrganization(owner, orgName, slug);
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + organizationId + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String first = createQuestion(owner, organizationId, category, "First question", "EASY", 1);
        String second = createQuestion(owner, organizationId, category, "Second question", "HARD", 2);
        String assessmentId = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + organizationId + "/assessments",
                "{\"title\":\"Packaged Quiz\",\"description\":\"Offline copy\",\"timeLimitMinutes\":45,"
                        + "\"maxAttempts\":2,\"passingScore\":70,\"shuffleQuestions\":true,\"shuffleAnswers\":true,"
                        + "\"showResultsAfterCompletion\":false}",
                owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + organizationId + "/assessments/" + assessmentId
                + "/questions/" + first, "{\"points\":1,\"displayOrder\":0}", owner);
        api.send("POST", "/api/v1/organizations/" + organizationId + "/assessments/" + assessmentId
                + "/questions/" + second, "{\"points\":3,\"displayOrder\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + organizationId + "/assessments/" + assessmentId + "/publish",
                null, owner);
        return new Fixture(owner, organizationId, assessmentId,
                "/api/v1/organizations/" + organizationId + "/assessments/" + assessmentId + "/package");
    }

    private String createQuestion(String token, String organizationId, String categoryId, String text,
                                  String difficulty, int optionSeed) throws Exception {
        var response = api.send("POST", "/api/v1/organizations/" + organizationId + "/questions",
                "{\"text\":\"" + text + "\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"" + difficulty
                        + "\",\"status\":\"ACTIVE\",\"categoryId\":\"" + categoryId
                        + "\",\"explanation\":\"Because\",\"options\":[{\"text\":\"Yes\",\"correct\":true,\"displayOrder\":0},"
                        + "{\"text\":\"No-" + optionSeed + "\",\"correct\":false,\"displayOrder\":1}]}",
                token);
        assertThat(response.statusCode()).isEqualTo(201);
        return ApiSupport.field(response.body(), "id");
    }

    private record Fixture(String owner, String organizationId, String assessmentId, String packagePath) {}
}
