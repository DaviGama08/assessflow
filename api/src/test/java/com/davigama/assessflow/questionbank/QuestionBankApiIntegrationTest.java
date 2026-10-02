package com.davigama.assessflow.questionbank;

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
class QuestionBankApiIntegrationTest {
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
    private String ownerA;
    private String ownerB;
    private String participant;
    private String orgA;
    private String orgB;
    private String categoriesA;
    private String questionsA;

    @BeforeEach
    void setUp() throws Exception {
        api = new ApiSupport(port);
        ownerA = api.register("qb-owner-a@example.com");
        ownerB = api.register("qb-owner-b@example.com");
        participant = api.register("qb-participant@example.com");
        orgA = api.createOrganization(ownerA, "Bank A", "bank-a");
        orgB = api.createOrganization(ownerB, "Bank B", "bank-b");
        api.addMember(ownerA, orgA, "qb-participant@example.com", "PARTICIPANT");
        categoriesA = "/api/v1/organizations/" + orgA + "/question-categories";
        questionsA = "/api/v1/organizations/" + orgA + "/questions";
    }

    @Test
    void managesQuestionsWithValidationFiltersArchiveAndTenantIsolation() throws Exception {
        var category = api.send("POST", categoriesA, "{\"name\":\"Networking\",\"slug\":\"Networking\"}", ownerA);
        assertThat(category.statusCode()).isEqualTo(201);
        assertThat(category.body()).contains("\"slug\":\"networking\"");
        String categoryId = ApiSupport.field(category.body(), "id");
        assertThat(api.send("POST", categoriesA, "{\"name\":\"Duplicate\",\"slug\":\"networking\"}", ownerA)
                .statusCode()).isEqualTo(409);
        var otherCategory = api.send("POST", "/api/v1/organizations/" + orgB + "/question-categories",
                "{\"name\":\"Networking\",\"slug\":\"networking\"}", ownerB);
        assertThat(otherCategory.statusCode()).isEqualTo(201);

        String singleChoice = question("What is TCP?", "SINGLE_CHOICE", "EASY", categoryId,
                "[{\"text\":\"Transport\",\"correct\":true},{\"text\":\"Physical\",\"correct\":false}]");
        var created = api.send("POST", questionsA, singleChoice, ownerA);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"type\":\"SINGLE_CHOICE\"", "\"correct\":true");
        String questionId = ApiSupport.field(created.body(), "id");

        assertThat(api.send("POST", questionsA, question("Bad SC", "SINGLE_CHOICE", "EASY", categoryId,
                "[{\"text\":\"A\",\"correct\":true},{\"text\":\"B\",\"correct\":true}]"), ownerA)
                .body()).contains("INVALID_QUESTION_OPTIONS");
        assertThat(api.send("POST", questionsA, question("Bad MC", "MULTIPLE_CHOICE", "MEDIUM", categoryId,
                "[{\"text\":\"A\",\"correct\":false},{\"text\":\"B\",\"correct\":false}]"), ownerA)
                .body()).contains("INVALID_QUESTION_OPTIONS");
        assertThat(api.send("POST", questionsA, question("Bad TF", "TRUE_FALSE", "HARD", categoryId,
                "[{\"text\":\"True\",\"correct\":true}]"), ownerA)
                .body()).contains("INVALID_QUESTION_OPTIONS");

        var multiple = api.send("POST", questionsA, question("Select layers", "MULTIPLE_CHOICE", "MEDIUM", categoryId,
                "[{\"text\":\"Network\",\"correct\":true},{\"text\":\"Transport\",\"correct\":true},{\"text\":\"Application\",\"correct\":false}]"),
                ownerA);
        assertThat(multiple.statusCode()).isEqualTo(201);
        var trueFalse = api.send("POST", questionsA, question("UDP is connectionless", "TRUE_FALSE", "EASY", categoryId,
                "[{\"text\":\"True\",\"correct\":true},{\"text\":\"False\",\"correct\":false}]"), ownerA);
        assertThat(trueFalse.statusCode()).isEqualTo(201);

        assertThat(api.send("GET", questionsA + "?search=TCP&type=SINGLE_CHOICE&difficulty=EASY&status=DRAFT&category="
                + categoryId, null, ownerA).body()).contains("What is TCP?").doesNotContain("Select layers");
        assertThat(api.send("GET", questionsA + "?size=101", null, ownerA).statusCode()).isEqualTo(400);
        var listed = api.send("GET", questionsA, null, ownerA);
        assertThat(listed.statusCode()).as(listed.body()).isEqualTo(200);
        assertThat(listed.body()).contains("What is TCP?", "Select layers", "UDP is connectionless");

        var updated = api.send("PUT", questionsA + "/" + questionId,
                question("What is TCP used for?", "SINGLE_CHOICE", "MEDIUM", categoryId,
                        "[{\"text\":\"Transport\",\"correct\":true},{\"text\":\"Link\",\"correct\":false}]")
                        .replace("\"text\":\"What is TCP used for?\"",
                                "\"text\":\"What is TCP used for?\",\"status\":\"ACTIVE\""),
                ownerA);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("What is TCP used for?", "\"status\":\"ACTIVE\"", "\"difficulty\":\"MEDIUM\"");

        assertThat(api.send("DELETE", questionsA + "/" + questionId, null, ownerA).statusCode()).isEqualTo(204);
        assertThat(api.send("GET", questionsA + "/" + questionId, null, ownerA).body()).contains("\"status\":\"ARCHIVED\"");
        assertThat(api.send("GET", questionsA + "?status=ARCHIVED", null, ownerA).body()).contains(questionId);

        assertThat(api.send("GET", questionsA + "/" + questionId, null, ownerB).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", "/api/v1/organizations/" + orgB + "/questions/" + questionId, null, ownerB)
                .statusCode()).isEqualTo(404);
        assertThat(api.send("PUT", "/api/v1/organizations/" + orgB + "/questions/" + questionId, singleChoice, ownerB)
                .statusCode()).isEqualTo(404);
        assertThat(api.send("DELETE", "/api/v1/organizations/" + orgB + "/questions/" + questionId, null, ownerB)
                .statusCode()).isEqualTo(404);
        assertThat(api.send("POST", questionsA, singleChoice, participant).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", questionsA, null, participant).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", categoriesA, null, participant).statusCode()).isEqualTo(403);
        assertThat(api.send("DELETE", categoriesA + "/" + categoryId, null, ownerA).body()).contains("CATEGORY_IN_USE");
    }

    private String question(String text, String type, String difficulty, String categoryId, String options) {
        return "{\"text\":\"" + text + "\",\"type\":\"" + type + "\",\"difficulty\":\"" + difficulty
                + "\",\"categoryId\":\"" + categoryId + "\",\"options\":" + options + "}";
    }
}
