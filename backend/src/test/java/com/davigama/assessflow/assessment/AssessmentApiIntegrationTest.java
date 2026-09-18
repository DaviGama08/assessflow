package com.davigama.assessflow.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import java.net.URI;
import java.util.UUID;
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
class AssessmentApiIntegrationTest {
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
    void scopesAssessmentsToOrganizationsAndEnforcesIsolation() throws Exception {
        String userA = api.register("owner-a@example.com");
        String userB = api.register("owner-b@example.com");
        String shared = api.register("shared@example.com");
        String participant = api.register("participant-a@example.com");
        String orgA = api.createOrganization(userA, "Org A", "org-a");
        String orgB = api.createOrganization(userB, "Org B", "org-b");
        api.addMember(userA, orgA, "shared@example.com", "INSTRUCTOR");
        api.addMember(userB, orgB, "shared@example.com", "INSTRUCTOR");
        api.addMember(userA, orgA, "participant-a@example.com", "PARTICIPANT");
        String pathA = "/api/v1/organizations/" + orgA + "/assessments";
        String pathB = "/api/v1/organizations/" + orgB + "/assessments";

        assertThat(api.send("GET", pathA, null, null).statusCode()).isEqualTo(401);
        assertThat(api.send("GET", "/api/v1/assessments", null, userA).statusCode()).isNotEqualTo(200);
        assertThat(api.send("POST", pathA, "{\"title\":\"   \"}", userA).statusCode()).isEqualTo(400);
        assertThat(api.send("GET", pathA + "/not-a-uuid", null, userA).body()).contains("INVALID_REQUEST");
        assertThat(api.send("GET", pathA + "?size=101", null, userA).statusCode()).isEqualTo(400);
        assertThat(api.send("POST", pathA, "{\"title\":\"Architecture\",\"timeLimitMinutes\":0}", userA)
                .statusCode()).isEqualTo(400);
        assertThat(api.send("POST", pathA, "{\"title\":\"Architecture\",\"maxAttempts\":0}", userA)
                .statusCode()).isEqualTo(400);
        assertThat(api.send("POST", pathA, "{\"title\":\"Architecture\",\"passingScore\":101}", userA)
                .statusCode()).isEqualTo(400);

        var created = api.send("POST", pathA,
                "{\"title\":\"Architecture\",\"description\":\"First draft\",\"timeLimitMinutes\":45,\"maxAttempts\":2,\"passingScore\":70,\"shuffleQuestions\":true,\"shuffleAnswers\":false,\"showResultsAfterCompletion\":true}",
                userA);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"status\":\"DRAFT\"", "\"maxAttempts\":2", "\"timeLimitMinutes\":45",
                "\"shuffleQuestions\":true", orgA);
        String assessmentPath = URI.create(created.headers().firstValue("Location").orElseThrow()).getPath();
        String assessmentId = ApiSupport.field(created.body(), "id");
        assertThat(api.send("GET", assessmentPath, null, userA).body()).contains("Architecture");
        assertThat(api.send("GET", pathA + "?page=0&size=10", null, userA).body()).contains("Architecture");

        var updated = api.send("PUT", assessmentPath,
                "{\"title\":\"Revised\",\"description\":\"Next draft\",\"timeLimitMinutes\":30,\"maxAttempts\":1,\"passingScore\":80,\"shuffleQuestions\":false,\"shuffleAnswers\":true,\"showResultsAfterCompletion\":false}",
                userA);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Revised", "\"passingScore\":80", "\"shuffleAnswers\":true");

        assertThat(api.send("GET", assessmentPath, null, userB).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", pathB + "/" + assessmentId, null, userB).statusCode()).isEqualTo(404);
        assertThat(api.send("PUT", pathB + "/" + assessmentId,
                "{\"title\":\"Hijack\",\"description\":\"no\"}", userB).statusCode()).isEqualTo(404);
        assertThat(api.send("DELETE", pathB + "/" + assessmentId, null, userB).statusCode()).isEqualTo(404);
        assertThat(api.send("GET", pathB, null, userB).body()).doesNotContain(assessmentId);
        assertThat(api.send("GET", assessmentPath, null, shared).statusCode()).isEqualTo(200);
        assertThat(api.send("GET", pathB + "/" + assessmentId, null, shared).statusCode()).isEqualTo(404);
        assertThat(api.send("POST", pathA, "{\"title\":\"Secret\"}", participant).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", pathA, null, participant).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", assessmentPath, null, participant).statusCode()).isEqualTo(403);

        var orgBAssessment = api.send("POST", pathB, "{\"title\":\"Org B Quiz\"}", userB);
        assertThat(orgBAssessment.statusCode()).isEqualTo(201);
        assertThat(api.send("GET", pathA, null, userA).body()).contains("Revised").doesNotContain("Org B Quiz");

        assertThat(api.send("DELETE", assessmentPath, null, userA).statusCode()).isEqualTo(204);
        var missing = api.send("GET", assessmentPath, null, userA);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.body()).contains("ASSESSMENT_NOT_FOUND");
        assertThat(api.send("GET", "/actuator/health", null, null).body()).contains("\"status\":\"UP\"");
        assertThat(api.send("GET", pathA + "/" + UUID.randomUUID(), null, userA).statusCode()).isEqualTo(404);
    }
}
