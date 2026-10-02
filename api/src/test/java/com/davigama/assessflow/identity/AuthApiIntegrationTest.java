package com.davigama.assessflow.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.identity.infrastructure.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
class AuthApiIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired UserRepository users;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void registrationLoginRefreshAndLogout() throws Exception {
        assertThat(send("GET", "/api/v1/auth/me", null, null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/organizations", null, null, null).statusCode()).isEqualTo(401);
        String registration = "{\"email\":\"  MEMBER@Example.com  \",\"password\":\"correct-password-123\",\"displayName\":\"Member\"}";
        var registered = send("POST", "/api/v1/auth/register", registration, null, null);
        assertThat(registered.statusCode()).isEqualTo(200);
        assertThat(registered.body()).contains("member@example.com").doesNotContain("passwordHash", "correct-password-123");
        var user = users.findByEmail("member@example.com").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("correct-password-123");
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(send("POST", "/api/v1/auth/register", registration, null, null).statusCode()).isEqualTo(409);
        String access = token(registered.body());
        String cookie = registered.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(cookie).contains("HttpOnly", "SameSite=Strict").doesNotContain("correct-password-123");
        assertThat(send("GET", "/api/v1/auth/me", null, access, null).body())
                .contains("member@example.com").doesNotContain("passwordHash", "accessToken");
        var wrong = send("POST", "/api/v1/auth/login",
                "{\"email\":\"member@example.com\",\"password\":\"wrong-password\"}", null, null);
        assertThat(wrong.statusCode()).isEqualTo(401);
        assertThat(wrong.body()).contains("INVALID_CREDENTIALS");
        var absent = send("POST", "/api/v1/auth/login",
                "{\"email\":\"absent@example.com\",\"password\":\"wrong-password\"}", null, null);
        assertThat(absent.statusCode()).isEqualTo(401);
        assertThat(absent.body()).contains("INVALID_CREDENTIALS");
        var login = send("POST", "/api/v1/auth/login",
                "{\"email\":\"MEMBER@example.com\",\"password\":\"correct-password-123\"}", null, null);
        assertThat(login.statusCode()).isEqualTo(200);
        var noOrigin = send("POST", "/api/v1/auth/refresh", null, null, cookie);
        assertThat(noOrigin.statusCode()).isEqualTo(400);
        var refreshed = send("POST", "/api/v1/auth/refresh", null, null, cookie, true);
        assertThat(refreshed.statusCode()).isEqualTo(200);
        assertThat(token(refreshed.body())).isNotEqualTo(access);
        assertThat(send("POST", "/api/v1/auth/refresh", null, null, cookie, true).statusCode()).isEqualTo(401);
        String newCookie = refreshed.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(send("POST", "/api/v1/auth/logout", null, token(refreshed.body()), newCookie, true).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/v1/auth/me", null, token(refreshed.body()), null).statusCode()).isEqualTo(401);
    }
    private String token(String body) { return body.split("\"accessToken\":\"")[1].split("\"")[0]; }
    private HttpResponse<String> send(String method, String path, String body, String token, String cookie) throws Exception {
        return send(method, path, body, token, cookie, false);
    }
    private HttpResponse<String> send(String method, String path, String body, String token, String cookie,
                                      boolean origin) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (cookie != null) builder.header("Cookie", cookie.split(";")[0]);
        if (origin) builder.header("Origin", "http://localhost:5173");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
