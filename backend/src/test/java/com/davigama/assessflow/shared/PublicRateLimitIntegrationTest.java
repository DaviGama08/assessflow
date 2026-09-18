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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.preview-per-minute=2",
                "app.rate-limit.auth-per-minute=2",
                "app.redis.enabled=false"
        })
@Testcontainers
@ActiveProfiles("test")
class PublicRateLimitIntegrationTest {
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
    void limitsPublicPreviewWithoutRedis() throws Exception {
        assertThat(api.send("GET", "/api/v1/live-sessions/preview?code=AAAAAA", null, null).statusCode())
                .isNotEqualTo(429);
        assertThat(api.send("GET", "/api/v1/live-sessions/preview?code=AAAAAA", null, null).statusCode())
                .isNotEqualTo(429);
        var limited = api.send("GET", "/api/v1/live-sessions/preview?code=AAAAAA", null, null);
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("RATE_LIMITED");
    }
}
