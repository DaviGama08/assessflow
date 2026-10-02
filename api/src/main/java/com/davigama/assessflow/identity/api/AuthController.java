package com.davigama.assessflow.identity.api;

import com.davigama.assessflow.identity.application.AuthException;
import com.davigama.assessflow.identity.application.AuthService;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.shared.config.RefreshCookieSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public record RegisterRequest(@NotBlank String email, @NotBlank String password,
                                  @NotBlank @Size(max = 200) String displayName) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UserResponse(UUID id, String email, String displayName, String status) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getStatus().name());
        }
    }
    public record AuthResponse(String accessToken, UserResponse user) {}
    private final AuthService service;
    private final RefreshCookieSettings cookies;
    private final String[] allowedOrigins;
    public AuthController(AuthService service, RefreshCookieSettings cookies,
                          @Value("${app.cors.allowed-origins}") String origins) {
        this.service = service;
        this.cookies = cookies;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
    }
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim();
        if (email.length() > 320 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new AuthException("INVALID_REQUEST", "Invalid email address.");
        if (request.password().length() < 12)
            throw new AuthException("INVALID_REQUEST", "Password must contain at least 12 characters.");
        return response(service.register(email, request.password(), request.displayName()));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return response(service.login(request.email(), request.password()));
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "assessflow_refresh", required = false) String refresh,
                                                 HttpServletRequest request) {
        checkOrigin(request);
        return response(service.refresh(refresh));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                       @CookieValue(name = "assessflow_refresh", required = false) String refresh,
                                       HttpServletRequest request) {
        checkOrigin(request);
        service.logout(authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null, refresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.cookie("", 0).toString()).build();
    }
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from((User) authentication.getPrincipal());
    }
    private ResponseEntity<AuthResponse> response(AuthService.Session session) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.cookie(session.refreshToken(), 7 * 24 * 3600).toString())
                .body(new AuthResponse(session.accessToken(), UserResponse.from(session.user())));
    }
    private void checkOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || Arrays.stream(allowedOrigins).noneMatch(origin::equals))
            throw new AuthException("INVALID_ORIGIN", "Invalid request origin.");
    }
}
