package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class StompBrokerRelayIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> rabbit = new GenericContainer<>("rabbitmq:4.1-management-alpine")
            .withExposedPorts(61613)
            .withEnv("RABBITMQ_DEFAULT_USER", "assessflow")
            .withEnv("RABBITMQ_DEFAULT_PASS", "assessflow-test")
            .withCopyToContainer(MountableFile.forClasspathResource("rabbitmq/enabled_plugins"),
                    "/etc/rabbitmq/enabled_plugins")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void infra(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.realtime.broker-mode", () -> "relay");
        registry.add("app.realtime.relay.host", rabbit::getHost);
        registry.add("app.realtime.relay.port", () -> rabbit.getMappedPort(61613));
        registry.add("app.realtime.relay.username", () -> "assessflow");
        registry.add("app.realtime.relay.password", () -> "assessflow-test");
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
    void relayBrokerDeliversQuestionStarted() throws Exception {
        String owner = api.register("relay-owner@example.com");
        String org = api.createOrganization(owner, "Relay Org", "relay-org");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String question = ApiSupport.field(api.send("POST", "/api/v1/organizations/" + org + "/questions",
                "{\"text\":\"Q1\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"status\":\"ACTIVE\",\"categoryId\":\""
                        + category
                        + "\",\"options\":[{\"text\":\"Yes\",\"correct\":true},{\"text\":\"No\",\"correct\":false}]}",
                owner).body(), "id");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Relay Quiz\"}", owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                "{\"points\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        var created = api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions", null, owner);
        String sessionId = ApiSupport.field(created.body(), "id");
        String code = ApiSupport.field(created.body(), "joinCode");
        String participantToken = ApiSupport.field(api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"Guest\"}", null).body(), "participantToken");

        StompSession participant = connect(participantToken).get(10, TimeUnit.SECONDS);
        StompSession attacker = connect(participantToken).get(10, TimeUnit.SECONDS);
        CompletableFuture<String> started = new CompletableFuture<>();
        CompletableFuture<String> injected = new CompletableFuture<>();
        participant.subscribe("/topic/sessions/" + sessionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                String type = String.valueOf(((Map<String, Object>) payload).get("type"));
                if ("QUESTION_STARTED".equals(type)) started.complete("QUESTION_STARTED");
                if ("SESSION_FINISHED".equals(type)) injected.complete("SESSION_FINISHED");
            }
        });
        Thread.sleep(500);
        try {
            if (attacker.isConnected()) {
                attacker.send("/topic/sessions/" + sessionId,
                        Map.of("type", "SESSION_FINISHED", "payload", Map.of("status", "FINISHED")));
            }
        } catch (RuntimeException ignored) {
            // inbound interceptor may close the attacker session
        }
        Thread.sleep(500);
        assertThat(injected.isDone()).isFalse();
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start", null, owner);
        assertThat(started.get(15, TimeUnit.SECONDS)).isEqualTo("QUESTION_STARTED");
        assertThat(injected.isDone()).isFalse();
        if (participant.isConnected()) participant.disconnect();
        if (attacker.isConnected()) attacker.disconnect();
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
}
