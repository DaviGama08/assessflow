package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class WebSocketMessageInjectionSecurityTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.realtime.broker-mode", () -> "simple");
    }

    @LocalServerPort int port;
    private ApiSupport api;
    private WebSocketStompClient client;

    @BeforeEach
    void setUp() {
        api = new ApiSupport(port);
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @Test
    void clientSendIsRejectedAndServerNotificationsStillArrive() throws Exception {
        String owner = api.register("inject-owner@example.com");
        String org = api.createOrganization(owner, "Inject Org", "inject-org");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String question = ApiSupport.field(api.send("POST", "/api/v1/organizations/" + org + "/questions",
                "{\"text\":\"Q1\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"status\":\"ACTIVE\",\"categoryId\":\""
                        + category
                        + "\",\"options\":[{\"text\":\"Yes\",\"correct\":true},{\"text\":\"No\",\"correct\":false}]}",
                owner).body(), "id");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Inject Quiz\"}", owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                "{\"points\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        var created = api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions", null, owner);
        String sessionId = ApiSupport.field(created.body(), "id");
        String code = ApiSupport.field(created.body(), "joinCode");
        String participantToken = ApiSupport.field(api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"Guest\"}", null).body(), "participantToken");

        Set<String> guestTypes = ConcurrentHashMap.newKeySet();
        Set<String> hostTypes = ConcurrentHashMap.newKeySet();
        CopyOnWriteArrayList<String> guestOrder = new CopyOnWriteArrayList<>();
        CompletableFuture<String> started = new CompletableFuture<>();
        CompletableFuture<String> questionStarted = new CompletableFuture<>();
        CompletableFuture<String> answerReceived = new CompletableFuture<>();
        CompletableFuture<String> results = new CompletableFuture<>();
        CompletableFuture<String> finished = new CompletableFuture<>();

        StompSession host = connect(owner).get(5, TimeUnit.SECONDS);
        StompSession guest = connect(participantToken).get(5, TimeUnit.SECONDS);
        StompSession participantAttacker = connect(participantToken).get(5, TimeUnit.SECONDS);
        StompSession hostAttacker = connect(owner).get(5, TimeUnit.SECONDS);

        guest.subscribe("/topic/sessions/" + sessionId, collecting(guestTypes, guestOrder, Map.of(
                "SESSION_STARTED", started,
                "QUESTION_STARTED", questionStarted,
                "QUESTION_RESULTS", results,
                "SESSION_FINISHED", finished)));
        host.subscribe("/topic/host/sessions/" + sessionId, collecting(hostTypes, new CopyOnWriteArrayList<>(), Map.of(
                "ANSWER_RECEIVED", answerReceived,
                "QUESTION_STARTED", new CompletableFuture<>())));
        Thread.sleep(300);

        Map<String, Object> fakeFinish = Map.of("type", "SESSION_FINISHED", "payload", Map.of("status", "FINISHED"));
        Map<String, Object> fakeQuestion = Map.of("type", "QUESTION_STARTED", "payload", Map.of("injected", true));
        sendQuietly(participantAttacker, "/topic/sessions/" + sessionId, fakeFinish);
        sendQuietly(participantAttacker, "/topic/host/sessions/" + sessionId, fakeQuestion);
        sendQuietly(hostAttacker, "/topic/sessions/" + sessionId, fakeFinish);
        sendQuietly(hostAttacker, "/app/commands", fakeQuestion);
        Thread.sleep(500);

        assertThat(guestTypes).isEmpty();
        assertThat(hostTypes).doesNotContain("SESSION_FINISHED", "QUESTION_STARTED");
        assertThat(guest.isConnected()).isTrue();
        assertThat(host.isConnected()).isTrue();

        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start", null, owner);
        assertThat(started.get(5, TimeUnit.SECONDS)).isEqualTo("SESSION_STARTED");
        assertThat(questionStarted.get(5, TimeUnit.SECONDS)).isEqualTo("QUESTION_STARTED");

        var state = api.send("GET", "/api/v1/live-sessions/" + sessionId + "/state", null, participantToken);
        String optionId = firstOption(state.body());
        assertThat(api.send("POST", "/api/v1/live-sessions/" + sessionId + "/answers",
                "{\"optionIds\":[\"" + optionId + "\"]}", participantToken).statusCode()).isEqualTo(204);
        assertThat(answerReceived.get(5, TimeUnit.SECONDS)).isEqualTo("ANSWER_RECEIVED");

        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/questions/end",
                null, owner);
        assertThat(results.get(5, TimeUnit.SECONDS)).isEqualTo("QUESTION_RESULTS");
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/finish", null, owner);
        assertThat(finished.get(5, TimeUnit.SECONDS)).isEqualTo("SESSION_FINISHED");
        assertThat(guestOrder).contains("SESSION_STARTED", "QUESTION_STARTED", "QUESTION_RESULTS", "SESSION_FINISHED");

        disconnectQuietly(host);
        disconnectQuietly(guest);
        disconnectQuietly(participantAttacker);
        disconnectQuietly(hostAttacker);
    }

    private void sendQuietly(StompSession session, String destination, Object payload) {
        try {
            if (session.isConnected()) {
                session.send(destination, payload);
            }
        } catch (RuntimeException ignored) {
            // inbound interceptor may close the attacker session
        }
    }

    private void disconnectQuietly(StompSession session) {
        try {
            if (session.isConnected()) session.disconnect();
        } catch (RuntimeException ignored) {
            // already closed after a rejected SEND
        }
    }

    private CompletableFuture<StompSession> connect(String token) {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                                byte[] payload, Throwable exception) {
                        // rejected SEND surfaces as an ERROR; do not fail the test thread
                    }
                });
    }

    private StompFrameHandler collecting(Set<String> types, CopyOnWriteArrayList<String> order,
                                         Map<String, CompletableFuture<String>> waiters) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                String type = String.valueOf(((Map<String, Object>) payload).get("type"));
                types.add(type);
                order.add(type);
                CompletableFuture<String> waiter = waiters.get(type);
                if (waiter != null) waiter.complete(type);
            }
        };
    }

    private String firstOption(String json) {
        int start = json.indexOf("\"options\":[{\"id\":\"");
        assertThat(start).isNotNegative();
        int from = start + "\"options\":[{\"id\":\"".length();
        return json.substring(from, json.indexOf('"', from));
    }
}
