package com.davigama.assessflow.assessment;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AssessmentLifecycleIntegrationTest {
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
    void publishesAndArchivesWithTenantAndRoleRules() throws Exception {
        String ownerA = api.register("life-owner-a@example.com");
        String ownerB = api.register("life-owner-b@example.com");
        String participant = api.register("life-participant@example.com");
        String orgA = api.createOrganization(ownerA, "Life A", "life-a");
        String orgB = api.createOrganization(ownerB, "Life B", "life-b");
        api.addMember(ownerA, orgA, "life-participant@example.com", "PARTICIPANT");
        String assessmentId = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + orgA + "/assessments",
                "{\"title\":\"Lifecycle\"}", ownerA).body(), "id");
        String path = "/api/v1/organizations/" + orgA + "/assessments/" + assessmentId;
        assertThat(api.send("POST", path + "/publish", null, ownerA).body())
                .contains("ASSESSMENT_HAS_NO_QUESTIONS");
        String categoryId = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + orgA + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", ownerA).body(), "id");
        String draftQuestion = createQuestion(ownerA, orgA, categoryId, "Draft item", "DRAFT");
        api.send("POST", path + "/questions/" + draftQuestion, "{\"points\":1}", ownerA);
        assertThat(api.send("POST", path + "/publish", null, ownerA).body())
                .contains("ASSESSMENT_HAS_INVALID_QUESTIONS");
        api.send("DELETE", path + "/questions/" + draftQuestion, null, ownerA);
        String activeQuestion = createQuestion(ownerA, orgA, categoryId, "Active item", "ACTIVE");
        assertThat(api.send("POST", path + "/questions/" + activeQuestion, "{\"points\":1}", ownerA)
                .statusCode()).isEqualTo(201);
        var published = api.send("POST", path + "/publish", null, ownerA);
        assertThat(published.statusCode()).isEqualTo(200);
        assertThat(published.body()).contains("\"status\":\"PUBLISHED\"");
        assertThat(api.send("POST", path + "/publish", null, ownerA).body())
                .contains("INVALID_ASSESSMENT_STATUS_TRANSITION");
        assertThat(api.send("POST", path + "/publish", null, participant).statusCode()).isEqualTo(403);
        assertThat(api.send("POST", "/api/v1/organizations/" + orgB + "/assessments/" + assessmentId + "/publish",
                null, ownerB).statusCode()).isEqualTo(404);
        var archived = api.send("POST", path + "/archive", null, ownerA);
        assertThat(archived.statusCode()).isEqualTo(200);
        assertThat(archived.body()).contains("\"status\":\"ARCHIVED\"");
        assertThat(api.send("POST", path + "/archive", null, ownerA).body())
                .contains("INVALID_ASSESSMENT_STATUS_TRANSITION");
        assertThat(api.send("POST", path + "/publish", null, ownerA).body())
                .contains("INVALID_ASSESSMENT_STATUS_TRANSITION");
    }

    private String createQuestion(String token, String organizationId, String categoryId, String text, String status)
            throws Exception {
        var response = api.send("POST", "/api/v1/organizations/" + organizationId + "/questions",
                "{\"text\":\"" + text + "\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"status\":\""
                        + status + "\",\"categoryId\":\"" + categoryId
                        + "\",\"options\":[{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":false}]}",
                token);
        assertThat(response.statusCode()).isEqualTo(201);
        return ApiSupport.field(response.body(), "id");
    }
}
