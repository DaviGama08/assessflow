package com.davigama.assessflow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApiSupport {
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final int port;

    public ApiSupport(int port) {
        this.port = port;
    }

    public String register(String email) throws Exception {
        var response = send("POST", "/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"secure-password-123\",\"displayName\":\"Test User\"}",
                null);
        if (response.statusCode() != 200) {
            throw new AssertionError("register failed: " + response.body());
        }
        return field(response.body(), "accessToken");
    }

    public String createOrganization(String token, String name, String slug) throws Exception {
        var response = send("POST", "/api/v1/organizations",
                "{\"name\":\"" + name + "\",\"slug\":\"" + slug + "\"}", token);
        if (response.statusCode() != 201) {
            throw new AssertionError("create organization failed: " + response.body());
        }
        return field(response.body(), "id");
    }

    public String addMember(String token, String organizationId, String email, String role) throws Exception {
        var response = send("POST", "/api/v1/organizations/" + organizationId + "/members",
                "{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}", token);
        if (response.statusCode() != 201) {
            throw new AssertionError("add member failed: " + response.body());
        }
        return field(response.body(), "id");
    }

    public HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else if ("GET".equals(method)) {
            builder.GET();
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static String field(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) throw new AssertionError("field " + name + " in " + json);
        return matcher.group(1);
    }
}
