package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import com.davigama.assessflow.livesession.application.ParticipantAuthService;
import com.davigama.assessflow.livesession.infrastructure.LiveParticipantRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class LiveSessionApiIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired LiveParticipantRepository participantRepository;
    private ApiSupport api;

    @BeforeEach
    void setUp() {
        api = new ApiSupport(port);
    }

    @Test
    void runsLiveSessionFromPublishThroughAnswerAndFinish() throws Exception {
        String owner = api.register("live-owner@example.com");
        String outsider = api.register("live-outsider@example.com");
        String org = api.createOrganization(owner, "Live Org", "live-org");
        String other = api.createOrganization(outsider, "Other Live", "other-live");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String q1 = createQuestion(owner, org, category, "Q1");
        String q2 = createQuestion(owner, org, category, "Q2");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Live Quiz\"}", owner).body(), "id");
        assertThat(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions",
                null, owner).body()).contains("ASSESSMENT_NOT_PUBLISHED");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + q1,
                "{\"points\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + q2,
                "{\"points\":2}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        var created = api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions",
                null, owner);
        assertThat(created.statusCode()).isEqualTo(201);
        String sessionId = ApiSupport.field(created.body(), "id");
        String code = ApiSupport.field(created.body(), "joinCode");
        assertThat(api.send("POST",
                "/api/v1/organizations/" + other + "/live-sessions/" + sessionId + "/start",
                null, outsider).statusCode()).isEqualTo(404);
        var preview = api.send("GET", "/api/v1/live-sessions/preview?code=" + code, null, null);
        assertThat(preview.statusCode()).isEqualTo(200);
        assertThat(preview.body()).contains("Live Quiz", "WAITING");
        var joined = api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code.toLowerCase() + "\",\"displayName\":\"Davi\"}", null);
        assertThat(joined.statusCode()).isEqualTo(200);
        String participantToken = ApiSupport.field(joined.body(), "participantToken");
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start", null, owner);
        var state = api.send("GET", "/api/v1/live-sessions/" + sessionId + "/state", null, participantToken);
        assertThat(state.statusCode()).isEqualTo(200);
        assertThat(state.body()).doesNotContain("\"correct\"");
        String optionId = firstOption(state.body());
        String otherOption = secondOption(state.body());
        assertThat(api.send("POST", "/api/v1/live-sessions/" + sessionId + "/answers",
                "{\"optionIds\":[\"" + optionId + "\",\"" + otherOption + "\"]}", participantToken).body())
                .contains("INVALID_ANSWER_SELECTION");
        assertThat(api.send("POST", "/api/v1/live-sessions/" + sessionId + "/answers",
                "{\"optionIds\":[\"" + optionId + "\",\"" + optionId + "\"]}", participantToken).body())
                .contains("INVALID_ANSWER_SELECTION");
        assertThat(api.send("GET", "/api/v1/live-sessions/" + sessionId + "/state", null, null).statusCode())
                .isEqualTo(401);
        var first = api.send("POST", "/api/v1/live-sessions/" + sessionId + "/answers",
                "{\"optionIds\":[\"" + optionId + "\"]}", participantToken);
        assertThat(first.statusCode()).isEqualTo(204);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> submit = () -> api.send("POST", "/api/v1/live-sessions/" + sessionId + "/answers",
                    "{\"optionIds\":[\"" + optionId + "\"]}", participantToken).statusCode();
            List<Integer> codes = new ArrayList<>();
            for (var future : pool.invokeAll(List.of(submit, submit))) codes.add(future.get());
            assertThat(codes).contains(409);
        }
        var recovered = api.send("GET", "/api/v1/live-sessions/" + sessionId + "/state", null, participantToken);
        assertThat(recovered.body()).contains("\"alreadyAnswered\":true");
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/questions/end",
                null, owner);
        var results = api.send("GET", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/results",
                null, owner);
        assertThat(results.body()).contains("\"correct\":true");
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/questions/next",
                null, owner);
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/questions/end",
                null, owner);
        var finished = api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/finish",
                null, owner);
        assertThat(finished.body()).contains("FINISHED");
        assertThat(api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"Late\"}", null).body())
                .contains("LIVE_SESSION_NOT_JOINABLE");
    }

    @Test
    void participantCannotCreateAndStartIsIdempotent() throws Exception {
        String owner = api.register("live-roles@example.com");
        String participant = api.register("live-participant@example.com");
        String org = api.createOrganization(owner, "Role Org", "role-org");
        api.addMember(owner, org, "live-participant@example.com", "PARTICIPANT");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String question = createQuestion(owner, org, category, "Only");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Roles Quiz\"}", owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                "{\"points\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        assertThat(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions",
                null, participant).statusCode()).isEqualTo(403);
        String sessionId = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions",
                null, owner).body(), "id");
        assertThat(api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start",
                null, owner).statusCode()).isEqualTo(200);
        assertThat(api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start",
                null, owner).body()).contains("LIVE_SESSION_ALREADY_STARTED");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/archive", null, owner);
        assertThat(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions",
                null, owner).body()).contains("ASSESSMENT_NOT_PUBLISHED");
    }

    @Test
    void leaveAndExpiredTokenAreRejectedAfterwards() throws Exception {
        String owner = api.register("live-token@example.com");
        String org = api.createOrganization(owner, "Token Org", "token-org");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String question = createQuestion(owner, org, category, "Only");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Token Quiz\"}", owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                "{\"points\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        var created = api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions",
                null, owner);
        String sessionId = ApiSupport.field(created.body(), "id");
        String code = ApiSupport.field(created.body(), "joinCode");
        var joined = api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"Guest\"}", null);
        String token = ApiSupport.field(joined.body(), "participantToken");
        assertThat(api.send("POST", "/api/v1/live-sessions/" + sessionId + "/leave", null, token).statusCode())
                .isEqualTo(204);
        assertThat(api.send("GET", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/participants",
                null, owner).body()).contains("LEFT");
        var guest = participantRepository.findByTokenHash(ParticipantAuthService.hash(token)).orElseThrow();
        guest.expireAt(Instant.EPOCH);
        participantRepository.save(guest);
        assertThat(api.send("GET", "/api/v1/live-sessions/" + sessionId + "/state", null, token).body())
                .contains("PARTICIPANT_TOKEN_EXPIRED");
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

    private String firstOption(String json) {
        int start = json.indexOf("\"options\":[{\"id\":\"");
        assertThat(start).isNotNegative();
        int from = start + "\"options\":[{\"id\":\"".length();
        return json.substring(from, json.indexOf('"', from));
    }

    private String secondOption(String json) {
        int first = json.indexOf("\"options\":[{\"id\":\"");
        int second = json.indexOf("{\"id\":\"", first + 10);
        assertThat(second).isNotNegative();
        int from = second + "{\"id\":\"".length();
        return json.substring(from, json.indexOf('"', from));
    }
}
