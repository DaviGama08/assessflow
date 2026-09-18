package com.davigama.assessflow.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.ApiSupport;
import org.junit.jupiter.api.Test;
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
class BrandingApiIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;

    @Test
    void restrictsBrandingUpdatesAndIsolatesTenants() throws Exception {
        ApiSupport api = new ApiSupport(port);
        String owner = api.register("brand-owner@example.com");
        String admin = api.register("brand-admin@example.com");
        String instructor = api.register("brand-instructor@example.com");
        String outsider = api.register("brand-outsider@example.com");
        String orgA = api.createOrganization(owner, "Solidus Training", "solidus");
        String orgB = api.createOrganization(outsider, "Other", "other-brand");
        api.addMember(owner, orgA, "brand-admin@example.com", "ADMIN");
        api.addMember(owner, orgA, "brand-instructor@example.com", "INSTRUCTOR");
        String brandingA = "/api/v1/organizations/" + orgA + "/branding";
        String dashboardA = "/api/v1/organizations/" + orgA + "/dashboard";

        var defaults = api.send("GET", brandingA, null, owner);
        assertThat(defaults.statusCode()).isEqualTo(200);
        assertThat(defaults.body()).contains("Solidus Training");
        var updated = api.send("PUT", brandingA,
                "{\"displayName\":\"Solidus Workspace\",\"logoUrl\":\"https://cdn.example.com/logo.png\",\"primaryColor\":\"#112233\",\"secondaryColor\":\"#abcdef\"}",
                owner);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Solidus Workspace", "#112233", "#ABCDEF");
        assertThat(api.send("PUT", brandingA,
                "{\"displayName\":\"Admin Brand\",\"logoUrl\":\"https://cdn.example.com/a.png\",\"primaryColor\":\"#445566\",\"secondaryColor\":\"#778899\"}",
                admin).statusCode()).isEqualTo(200);
        assertThat(api.send("PUT", brandingA,
                "{\"displayName\":\"No\",\"primaryColor\":\"#445566\",\"secondaryColor\":\"#778899\"}", instructor)
                .statusCode()).isEqualTo(403);
        assertThat(api.send("PUT", brandingA,
                "{\"displayName\":\"Bad\",\"primaryColor\":\"red\",\"secondaryColor\":\"#778899\"}", owner)
                .body()).contains("INVALID_COLOR");
        assertThat(api.send("PUT", brandingA,
                "{\"displayName\":\"Bad\",\"logoUrl\":\"javascript:alert(1)\",\"primaryColor\":\"#445566\"}", owner)
                .body()).contains("INVALID_LOGO_URL");
        assertThat(api.send("GET", brandingA, null, outsider).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", "/api/v1/organizations/" + orgB + "/branding", null, outsider).body())
                .contains("Other")
                .doesNotContain("Admin Brand");
        var dashboard = api.send("GET", dashboardA, null, instructor);
        assertThat(dashboard.statusCode()).isEqualTo(200);
        assertThat(dashboard.body()).contains("\"assessmentCount\":0", "\"questionCount\":0", "\"memberCount\":3");
        assertThat(api.send("PATCH", "/api/v1/organizations/" + orgA,
                "{\"name\":\"Solidus Training Co\"}", instructor).statusCode()).isEqualTo(403);
        assertThat(api.send("PATCH", "/api/v1/organizations/" + orgA,
                "{\"name\":\"Solidus Training Co\"}", owner).body()).contains("Solidus Training Co");
    }
}
