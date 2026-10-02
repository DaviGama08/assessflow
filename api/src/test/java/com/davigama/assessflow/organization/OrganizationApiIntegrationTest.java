package com.davigama.assessflow.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
class OrganizationApiIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @LocalServerPort int port;
    @Autowired OrganizationRepository organizations;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void managesMembershipsAndProtectsLastOwner() throws Exception {
        String owner = register("owner@example.com");
        String admin = register("admin@example.com");
        String instructor = register("instructor@example.com");
        String participant = register("participant@example.com");
        String outsider = register("outsider@example.com");
        String base = "/api/v1/organizations";
        assertThat(send("GET", base, null, null).statusCode()).isEqualTo(401);
        var created = send("POST", base, "{\"name\":\"Alpha Team\",\"slug\":\"  Alpha Team  \"}", owner);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"slug\":\"alpha-team\"");
        String alpha = field(created.body(), "id");
        String path = base + "/" + alpha;
        assertThat(send("GET", path, null, outsider).statusCode()).isEqualTo(403);
        assertThat(send("GET", path + "/members", null, outsider).statusCode()).isEqualTo(403);
        assertThat(send("GET", path, null, owner).statusCode()).isEqualTo(200);
        var ownerMembers = send("GET", path + "/members", null, owner);
        assertThat(ownerMembers.body()).contains("\"role\":\"OWNER\"", "owner@example.com");
        String ownerMemberId = field(ownerMembers.body(), "id");
        assertThat(send("POST", base, "{\"name\":\"Duplicate\",\"slug\":\"alpha-team\"}", outsider).statusCode())
                .isEqualTo(409);
        assertThat(organizations.count()).isEqualTo(1);
        var privateOrg = send("POST", base, "{\"name\":\"Private\",\"slug\":\"private\"}", outsider);
        assertThat(privateOrg.statusCode()).isEqualTo(201);
        assertThat(send("GET", base, null, owner).body()).contains(alpha).doesNotContain("private");
        assertThat(send("GET", base, null, outsider).body()).contains("private").doesNotContain(alpha);
        assertThat(send("POST", path + "/members", "{\"email\":\"absent@example.com\",\"role\":\"PARTICIPANT\"}", owner)
                .statusCode()).isEqualTo(404);

        String adminMemberId = add(path, owner, "admin@example.com", "ADMIN");
        assertThat(send("POST", path + "/members", "{\"email\":\"admin@example.com\",\"role\":\"ADMIN\"}", owner)
                .body()).contains("MEMBERSHIP_EXISTS");
        String instructorMemberId = add(path, owner, "instructor@example.com", "INSTRUCTOR");
        String participantMemberId = add(path, owner, "participant@example.com", "PARTICIPANT");
        assertThat(send("POST", path + "/members", "{\"email\":\"outsider@example.com\",\"role\":\"PARTICIPANT\"}", instructor)
                .statusCode()).isEqualTo(403);
        assertThat(send("DELETE", path + "/members/" + participantMemberId, null, participant).statusCode()).isEqualTo(403);
        assertThat(send("PATCH", path + "/members/" + instructorMemberId + "/role",
                "{\"role\":\"PARTICIPANT\"}", admin).statusCode()).isEqualTo(200);
        assertThat(send("PATCH", path + "/members/" + adminMemberId + "/role",
                "{\"role\":\"OWNER\"}", admin).statusCode()).isEqualTo(403);
        assertThat(send("PATCH", path + "/members/" + ownerMemberId + "/role",
                "{\"role\":\"ADMIN\"}", admin).statusCode()).isEqualTo(403);
        assertThat(send("DELETE", path + "/members/" + ownerMemberId, null, admin).statusCode()).isEqualTo(403);
        assertThat(send("PATCH", path + "/members/" + ownerMemberId + "/role",
                "{\"role\":\"ADMIN\"}", owner).body()).contains("LAST_OWNER");
        assertThat(send("DELETE", path + "/members/" + ownerMemberId, null, owner).body()).contains("LAST_OWNER");
        assertThat(send("DELETE", path + "/members/" + participantMemberId, null, admin).statusCode()).isEqualTo(204);
        assertThat(send("GET", path + "/members", null, owner).body()).doesNotContain("participant@example.com");
        String restoredId = add(path, admin, "participant@example.com", "PARTICIPANT");
        assertThat(restoredId).isEqualTo(participantMemberId);
        assertThat(send("PATCH", path + "/members/" + adminMemberId + "/role",
                "{\"role\":\"OWNER\"}", owner).statusCode()).isEqualTo(200);
        assertThat(send("PATCH", path + "/members/" + ownerMemberId + "/role",
                "{\"role\":\"ADMIN\"}", owner).statusCode()).isEqualTo(200);
        assertThat(send("DELETE", path + "/members/" + adminMemberId, null, admin).body()).contains("LAST_OWNER");
    }
    private String register(String email) throws Exception {
        var response = send("POST", "/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"secure-password-123\",\"displayName\":\"Test User\"}", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return field(response.body(), "accessToken");
    }
    private String add(String path, String actor, String email, String role) throws Exception {
        var response = send("POST", path + "/members",
                "{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}", actor);
        assertThat(response.statusCode()).isEqualTo(201);
        return field(response.body(), "id");
    }
    private String field(String json, String name) {
        Matcher matcher = Pattern.compile("\\\"" + name + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        assertThat(matcher.find()).as("field " + name + " in " + json).isTrue();
        return matcher.group(1);
    }
    private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
