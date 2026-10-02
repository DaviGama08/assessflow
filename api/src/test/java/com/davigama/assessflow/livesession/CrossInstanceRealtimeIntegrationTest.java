package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import com.davigama.assessflow.AssessFlowApplication;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class CrossInstanceRealtimeIntegrationTest {
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

    @Test
    void participantOnInstanceAReceivesHostCommandFromInstanceB() throws Exception {
        int portA = freePort();
        int portB = freePort();
        String[] shared = {
                "--spring.profiles.active=test",
                "--spring.jmx.enabled=false",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--app.realtime.broker-mode=relay",
                "--app.realtime.relay.host=" + rabbit.getHost(),
                "--app.realtime.relay.port=" + rabbit.getMappedPort(61613),
                "--app.realtime.relay.username=assessflow",
                "--app.realtime.relay.password=assessflow-test",
                "--app.redis.enabled=false"
        };
        try (ConfigurableApplicationContext ignoredA = new SpringApplicationBuilder(AssessFlowApplication.class)
                .properties("server.port=" + portA).run(shared);
             ConfigurableApplicationContext ignoredB = new SpringApplicationBuilder(AssessFlowApplication.class)
                     .properties("server.port=" + portB).run(shared)) {
            ApiSupport apiA = new ApiSupport(portA);
            ApiSupport apiB = new ApiSupport(portB);
            String owner = apiA.register("cross-owner@example.com");
            String org = apiA.createOrganization(owner, "Cross Org", "cross-org");
            String category = ApiSupport.field(apiA.send("POST",
                    "/api/v1/organizations/" + org + "/question-categories",
                    "{\"name\":\"Core\",\"slug\":\"core\"}", owner).body(), "id");
            String question = ApiSupport.field(apiA.send("POST", "/api/v1/organizations/" + org + "/questions",
                    "{\"text\":\"Q1\",\"type\":\"SINGLE_CHOICE\",\"difficulty\":\"EASY\",\"status\":\"ACTIVE\",\"categoryId\":\""
                            + category
                            + "\",\"options\":[{\"text\":\"Yes\",\"correct\":true},{\"text\":\"No\",\"correct\":false}]}",
                    owner).body(), "id");
            String assessment = ApiSupport.field(apiA.send("POST",
                    "/api/v1/organizations/" + org + "/assessments", "{\"title\":\"Cross Quiz\"}", owner).body(), "id");
            apiA.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/questions/" + question,
                    "{\"points\":1}", owner);
            apiA.send("POST", "/api/v1/organizations/" + org + "/assessments/" + assessment + "/publish", null, owner);
            var created = apiA.send("POST",
                    "/api/v1/organizations/" + org + "/assessments/" + assessment + "/live-sessions", null, owner);
            String sessionId = ApiSupport.field(created.body(), "id");
            String code = ApiSupport.field(created.body(), "joinCode");
            String participantToken = ApiSupport.field(apiA.send("POST", "/api/v1/live-sessions/join",
                    "{\"code\":\"" + code + "\",\"displayName\":\"Guest\"}", null).body(), "participantToken");

            WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
            client.setMessageConverter(new JacksonJsonMessageConverter());
            StompHeaders headers = new StompHeaders();
            headers.add("Authorization", "Bearer " + participantToken);
            StompSession participant = client.connectAsync("ws://localhost:" + portA + "/ws",
                    new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
            CompletableFuture<String> started = new CompletableFuture<>();
            participant.subscribe("/topic/sessions/" + sessionId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void handleFrame(StompHeaders headers, Object payload) {
                    if ("QUESTION_STARTED".equals(String.valueOf(((Map<String, Object>) payload).get("type")))) {
                        started.complete("QUESTION_STARTED");
                    }
                }
            });
            Thread.sleep(800);
            assertThat(apiB.send("POST", "/api/v1/organizations/" + org + "/live-sessions/" + sessionId + "/start",
                    null, owner).statusCode()).isEqualTo(200);
            assertThat(started.get(20, TimeUnit.SECONDS)).isEqualTo("QUESTION_STARTED");
            if (participant.isConnected()) participant.disconnect();
        }
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
