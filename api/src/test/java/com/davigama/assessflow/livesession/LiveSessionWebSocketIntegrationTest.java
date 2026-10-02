package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.davigama.assessflow.ApiSupport;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class LiveSessionWebSocketIntegrationTest {
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
    private WebSocketStompClient client;

    @BeforeEach
    void setUp() {
        api = new ApiSupport(port);
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @Test
    void hostAndParticipantReceiveTypedEventsAndUnauthorizedTopicsAreRejected() throws Exception {
        String owner = api.register("ws-owner@example.com");
        String outsider = api.register("ws-outsider@example.com");
        String org = api.createOrganization(owner, "Ws Org", "ws-org");
        api.createOrganization(outsider, "Other Ws", "other-ws");
        String category = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/question-categories",
                "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
        String question = ApiSupport.field(api.send("POST", "/api/v1/organizations/" + org + "/questions",
                "{\"text\":\"Q1\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"status\":\"ACTIVE\",\"categoryId\":\""
                        + category
                        + "\",\"options\":[{\"text\":\"Yes\",\"correct\":true},{\"text\":\"No\",\"correct\":false}]}",
                owner).body(), "id");
        String assessment = ApiSupport.field(api.send("POST",
                "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Ws Quiz\"}", owner).body(), "id");
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                "{\"points\":1}", owner);
        api.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
        var created = api.send("POST",
                "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions", null, owner);
        String sessionId = ApiSupport.field(created.body(), "id");
        String code = ApiSupport.field(created.body(), "joinCode");
        var joined = api.send("POST", "/api/v1/live-sessions/join",
                "{\"code\":\"" + code + "\",\"displayName\":\"Guest\"}", null);
        String participantToken = ApiSupport.field(joined.body(), "participantToken");

        assertThatThrownBy(() -> connect("invalid-token").get(3, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(Exception.class);

        StompSession host = connect(owner).get(5, TimeUnit.SECONDS);
        StompSession participant = connect(participantToken).get(5, TimeUnit.SECONDS);
        CompletableFuture<String> hostQuestion = new CompletableFuture<>();
        CompletableFuture<String> participantQuestion = new CompletableFuture<>();
        CompletableFuture<String> participantFinished = new CompletableFuture<>();
        host.subscribe("/topic/host/sessions/" + sessionId, handler(hostQuestion, "QUESTION_STARTED"));
        participant.subscribe("/topic/sessions/" + sessionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                String type = String.valueOf(((Map<String, Object>) payload).get("type"));
                if ("QUESTION_STARTED".equals(type)) participantQuestion.complete(type);
                if ("SESSION_FINISHED".equals(type)) participantFinished.complete(type);
            }
        });

        CompletableFuture<String> leakedHostEvent = new CompletableFuture<>();
        StompSession other = connect(participantToken).get(5, TimeUnit.SECONDS);
        try {
            other.subscribe("/topic/host/sessions/" + sessionId, handler(leakedHostEvent, "QUESTION_STARTED"));
            other.subscribe("/topic/sessions/" + UUID.randomUUID(), handler(new CompletableFuture<>(), "SESSION_STARTED"));
        } catch (RuntimeException ignored) {
            // rejected subscribe may close the session
        }
        StompSession outsiderSession = connect(outsider).get(5, TimeUnit.SECONDS);
        CompletableFuture<String> outsiderLeak = new CompletableFuture<>();
        try {
            outsiderSession.subscribe("/topic/host/sessions/" + sessionId, handler(outsiderLeak, "QUESTION_STARTED"));
        } catch (RuntimeException ignored) {
            // rejected subscribe
        }

        Thread.sleep(300);
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start", null, owner);
        assertThat(hostQuestion.get(5, TimeUnit.SECONDS)).isEqualTo("QUESTION_STARTED");
        assertThat(participantQuestion.get(5, TimeUnit.SECONDS)).isEqualTo("QUESTION_STARTED");
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/questions/end", null, owner);
        api.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/finish", null, owner);
        assertThat(participantFinished.get(5, TimeUnit.SECONDS)).isEqualTo("SESSION_FINISHED");
        assertThat(leakedHostEvent.isDone()).isFalse();
        assertThat(outsiderLeak.isDone()).isFalse();
        disconnectQuietly(host);
        disconnectQuietly(participant);
        disconnectQuietly(other);
        disconnectQuietly(outsiderSession);
    }

    private void disconnectQuietly(StompSession session) {
        try {
            if (session.isConnected()) session.disconnect();
        } catch (RuntimeException ignored) {
            // already closed after a rejected subscribe
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
                        throw new IllegalStateException(exception);
                    }
                });
    }

    private StompFrameHandler handler(CompletableFuture<String> future, String expected) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                if (expected.equals(String.valueOf(((Map<String, Object>) payload).get("type")))) {
                    future.complete(expected);
                }
            }
        };
    }
}
