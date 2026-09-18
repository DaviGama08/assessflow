package com.davigama.assessflow.shared;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rate-limit.preview-per-minute=2")
@Testcontainers
@ActiveProfiles("test")
class RedisRateLimitIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void infra(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.redis.enabled", () -> "true");
        registry.add("app.redis.host", redis::getHost);
        registry.add("app.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort int port;
    private ApiSupport api;

    @BeforeEach
    void setUp() {
        api = new ApiSupport(port);
    }

    @Test
    void redisRateLimitRejectsThenFailsOpenIfRedisStops() throws Exception {
        assertThat(api.send("GET", "/api/v1/live-sessions/preview?code=BBBBBB", null, null).statusCode())
                .isNotEqualTo(429);
        assertThat(api.send("GET", "/api/v1/live-sessions/preview?code=BBBBBB", null, null).statusCode())
                .isNotEqualTo(429);
        assertThat(api.send("GET", "/api/v1/live-sessions/preview?code=BBBBBB", null, null).body())
                .contains("RATE_LIMITED");
        redis.stop();
        var open = api.send("GET", "/api/v1/live-sessions/preview?code=BBBBBB", null, null);
        assertThat(open.statusCode()).isNotEqualTo(429);
        assertThat(open.statusCode()).isNotEqualTo(500);
    }
}
