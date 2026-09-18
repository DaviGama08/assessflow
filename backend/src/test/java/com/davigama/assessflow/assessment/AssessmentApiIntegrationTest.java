package com.davigama.assessflow.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AssessmentApiIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private String accessToken;

    @Test
    void persistsAndManagesAssessmentThroughHttp() throws Exception {
        assertThat(send("GET", "/api/v1/assessments", null).statusCode()).isEqualTo(401);
        var registration = send("POST", "/api/v1/auth/register",
                "{\"email\":\"assessment@example.com\",\"password\":\"secure-password-123\",\"displayName\":\"Assessment Tester\"}");
        assertThat(registration.statusCode()).isEqualTo(200);
        accessToken = registration.body().split("\"accessToken\":\"")[1].split("\"")[0];
        var invalid = send("POST", "/api/v1/assessments", "{\"title\":\"   \"}");
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("INVALID_REQUEST");
        assertThat(send("GET", "/api/v1/assessments/not-a-uuid", null).body()).contains("INVALID_REQUEST");
        assertThat(send("GET", "/api/v1/assessments?size=101", null).statusCode()).isEqualTo(400);

        var created = send("POST", "/api/v1/assessments", "{\"title\":\"Architecture\",\"description\":\"First draft\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"status\":\"DRAFT\"");
        String path = URI.create(created.headers().firstValue("Location").orElseThrow()).getPath();
        assertThat(send("GET", path, null).body()).contains("Architecture");
        assertThat(send("GET", "/api/v1/assessments?page=0&size=10", null).body()).contains("Architecture");

        var updated = send("PUT", path, "{\"title\":\"Revised\",\"description\":\"Next draft\"}");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Revised", "Next draft");
        assertThat(send("GET", path, null).body()).contains("Revised");

        assertThat(send("DELETE", path, null).statusCode()).isEqualTo(204);
        var missing = send("GET", path, null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.body()).contains("ASSESSMENT_NOT_FOUND");
        assertThat(send("GET", "/actuator/health", null).body()).contains("\"status\":\"UP\"");
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
        HttpRequest request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
