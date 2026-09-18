package com.davigama.assessflow.identity.application;

import com.davigama.assessflow.identity.domain.AuthToken;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.identity.domain.UserStatus;
import com.davigama.assessflow.identity.infrastructure.AuthTokenRepository;
import com.davigama.assessflow.identity.infrastructure.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record Session(String accessToken, String refreshToken, User user) {}
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final Clock clock;

    public AuthService(UserRepository users, AuthTokenRepository tokens, PasswordEncoder passwords, Clock clock) {
        this.users = users; this.tokens = tokens; this.passwords = passwords; this.clock = clock;
    }
    @Transactional
    public Session register(String email, String password, String displayName) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email is already registered.");
        User user = new User(normalized, passwords.encode(password), displayName, clock.instant());
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException ex) {
            throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email is already registered.");
        }
        return issue(user);
    }
    @Transactional
    public Session login(String email, String password) {
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(AuthService::invalidCredentials);
        if (user.getStatus() != UserStatus.ACTIVE || !passwords.matches(password, user.getPasswordHash()))
            throw invalidCredentials();
        return issue(user);
    }
    @Transactional
    public Session refresh(String raw) {
        AuthToken token = valid(raw, AuthToken.Kind.REFRESH).orElseThrow(AuthService::invalidCredentials);
        tokens.delete(token);
        tokens.flush();
        return issue(token.getUser());
    }
    @Transactional
    public void logout(String access, String refresh) {
        if (access != null) tokens.deleteById(hash(access));
        if (refresh != null) tokens.deleteById(hash(refresh));
    }
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String raw) {
        return valid(raw, AuthToken.Kind.ACCESS).map(AuthToken::getUser);
    }
    private Optional<AuthToken> valid(String raw, AuthToken.Kind kind) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        return tokens.findByTokenHashAndKind(hash(raw), kind)
                .filter(token -> token.getExpiresAt().isAfter(clock.instant()))
                .filter(token -> token.getUser().getStatus() == UserStatus.ACTIVE);
    }
    private Session issue(User user) {
        String access = randomToken();
        String refresh = randomToken();
        tokens.save(new AuthToken(hash(access), user, AuthToken.Kind.ACCESS, clock.instant().plus(Duration.ofMinutes(15))));
        tokens.save(new AuthToken(hash(refresh), user, AuthToken.Kind.REFRESH, clock.instant().plus(Duration.ofDays(7))));
        return new Session(access, refresh, user);
    }
    private static String randomToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes);
    }
    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static AuthException invalidCredentials() {
        return new AuthException("INVALID_CREDENTIALS", "Invalid credentials.");
    }
}
