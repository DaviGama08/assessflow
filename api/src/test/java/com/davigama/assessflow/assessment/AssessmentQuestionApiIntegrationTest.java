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
class AssessmentQuestionApiIntegrationTest {
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

    @Test
    void linksReusableQuestionsWithOrderingAndBlocksCrossTenantAssociation() throws Exception {
        String ownerA = api.register("builder-a@example.com");
        String ownerB = api.register("builder-b@example.com");
        String instructor = api.register("builder-instructor@example.com");
        String participant = api.register("builder-participant@example.com");
        String orgA = api.createOrganization(ownerA, "Builder A", "builder-a");
        String orgB = api.createOrganization(ownerB, "Builder B", "builder-b");
        api.addMember(ownerA, orgA, "builder-instructor@example.com", "INSTRUCTOR");
        api.addMember(ownerA, orgA, "builder-participant@example.com", "PARTICIPANT");

        String categoryA = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + orgA + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", ownerA).body(), "id");
        String categoryB = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + orgB + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", ownerB).body(), "id");
        String questionOne = createQuestion(ownerA, orgA, categoryA, "Question one");
        String questionTwo = createQuestion(ownerA, orgA, categoryA, "Question two");
        String foreignQuestion = createQuestion(ownerB, orgB, categoryB, "Foreign question");
        String assessmentId = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + orgA + "/assessments",
                "{\"title\":\"Midterm\"}", ownerA).body(), "id");
        String links = "/api/v1/organizations/" + orgA + "/assessments/" + assessmentId + "/questions";

        var added = api.send("POST", links + "/" + questionOne, "{\"points\":2,\"displayOrder\":1}", ownerA);
        assertThat(added.statusCode()).isEqualTo(201);
        assertThat(added.body()).contains("Question one", "\"points\":2");
        assertThat(api.send("POST", links + "/" + questionOne, "{\"points\":2}", ownerA)
                .body()).contains("QUESTION_ALREADY_ADDED");
        assertThat(api.send("POST", links + "/" + questionTwo, "{\"points\":0}", ownerA).statusCode()).isEqualTo(400);
        assertThat(api.send("POST", links + "/" + questionTwo, "{\"points\":3,\"displayOrder\":2}", instructor)
                .statusCode()).isEqualTo(201);
        assertThat(api.send("POST", links + "/" + foreignQuestion, "{\"points\":1}", ownerA)
                .statusCode()).isEqualTo(404);
        assertThat(api.send("POST", links + "/" + questionOne, "{\"points\":1}", participant).statusCode())
                .isEqualTo(403);

        var listed = api.send("GET", links, null, ownerA);
        assertThat(listed.body()).contains("Question one", "Question two");
        var reordered = api.send("PUT", links + "/order",
                "{\"questionIds\":[\"" + questionTwo + "\",\"" + questionOne + "\"]}", ownerA);
        assertThat(reordered.statusCode()).isEqualTo(200);
        assertThat(reordered.body().indexOf("Question two")).isLessThan(reordered.body().indexOf("Question one"));
        var points = api.send("PATCH", links + "/" + questionTwo, "{\"points\":5}", ownerA);
        assertThat(points.statusCode()).isEqualTo(200);
        assertThat(points.body()).contains("\"points\":5");
        assertThat(api.send("DELETE", links + "/" + questionOne, null, ownerA).statusCode()).isEqualTo(204);
        assertThat(api.send("GET", links, null, ownerA).body()).contains("Question two").doesNotContain("Question one");
        assertThat(api.send("GET", "/api/v1/organizations/" + orgA + "/questions/" + questionOne, null, ownerA)
                .statusCode()).isEqualTo(200);
    }

    @BeforeEach
    void setUp() {
        api = new ApiSupport(port);
    }

    private String createQuestion(String token, String organizationId, String categoryId, String text) throws Exception {
        var response = api.send("POST", "/api/v1/organizations/" + organizationId + "/questions",
                "{\"text\":\"" + text + "\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"categoryId\":\""
                        + categoryId + "\",\"options\":[{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":false}]}",
                token);
        assertThat(response.statusCode()).isEqualTo(201);
        return ApiSupport.field(response.body(), "id");
    }
}
