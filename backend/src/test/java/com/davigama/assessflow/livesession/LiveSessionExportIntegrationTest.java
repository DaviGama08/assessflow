package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import java.nio.charset.StandardCharsets;
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
class LiveSessionExportIntegrationTest {
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
    void exportsFinishedSessionCsvAndRejectsActivePartialExport() throws Exception {
        String owner = api.register("csv-owner@example.com");
        String outsider = api.register("csv-outsider@example.com");
        String participantUser = api.register("csv-participant@example.com");
        String org = api.createOrganization(owner, "CSV Org", "csv-org");
        String other = api.createOrganization(outsider, "CSV Other", "csv-other");
        api.addMember(owner, org, "csv-participant@example.com", "PARTICIPANT");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String question = createQuestion(owner, org, category, "Only");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments",
                "{\"title\":\"Quiz, \\\"final\\\"\\nline\"}", owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                "{\"points\":2}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        var created = api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions", null, owner);
        String sessionId = ApiSupport.field(created.body(), "id");
        String code = ApiSupport.field(created.body(), "joinCode");
        String exportPath = "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/export";

        assertThat(api.send("GET", exportPath, null, owner).body()).contains("LIVE_SESSION_NOT_FINISHED");
        api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"=SUM(A1)\"}", null);
        api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"José, test\"}", null);
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start", null, owner);
        assertThat(api.send("GET", exportPath, null, owner).body()).contains("LIVE_SESSION_NOT_FINISHED");
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/questions/end", null, owner);
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/finish", null, owner);

        var exported = api.send("GET", exportPath, null, owner);
        assertThat(exported.statusCode()).isEqualTo(200);
        assertThat(exported.headers().firstValue("Content-Type").orElse(""))
                .contains("text/csv");
        String csv = exported.body();
        assertThat(csv.getBytes(StandardCharsets.UTF_8)).isEqualTo(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(csv).contains("\"Quiz, \"\"final\"\"\nline\"");
        assertThat(csv).contains("\"'=SUM(A1)\"");
        assertThat(csv).contains("\"José, test\"");
        assertThat(csv).doesNotContain(",=SUM(A1),");
        assertThat(csv).contains(sessionId);

        assertThat(api.send("GET", exportPath, null, null).statusCode()).isEqualTo(401);
        assertThat(api.send("GET", exportPath, null, participantUser).statusCode()).isEqualTo(403);
        assertThat(api.send("GET",
                "/api/v1/organizations/" + other + "/live-sessions/" + sessionId + "/export",
                null, outsider).statusCode()).isEqualTo(404);
    }

    private String createQuestion(String token, String organizationId, String categoryId, String text) throws Exception {
        var response = api.send("POST", "/api/v1/organizations/" + organizationId + "/questions",
                "{\"text\":\"" + text + "\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"status\":\"ACTIVE\",\"categoryId\":\""
                        + categoryId
                        + "\",\"options\":[{\"text\":\"Yes\",\"correct\":true},{\"text\":\"No\",\"correct\":false}]}",
                token);
        assertThat(response.statusCode()).isEqualTo(201);
        return ApiSupport.field(response.body(), "id");
    }
}
