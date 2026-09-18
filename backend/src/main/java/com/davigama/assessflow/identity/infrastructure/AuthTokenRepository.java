package com.davigama.assessflow.identity.infrastructure;

import com.davigama.assessflow.identity.domain.AuthToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByTokenHashAndKind(String hash, AuthToken.Kind kind);
    void deleteByUserId(UUID userId);
}
