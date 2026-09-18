package com.davigama.assessflow.shared;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.davigama.assessflow.shared.config.ProductionGuards;
import com.davigama.assessflow.shared.config.RefreshCookieSettings;
import org.junit.jupiter.api.Test;

class ProductionGuardsTest {
    @Test
    void rejectsLocalhostAndEmptyProductionInfrastructure() {
        assertThatThrownBy(() -> ProductionGuards.requireExplicitHost("REDIS_HOST", "localhost"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
        assertThatThrownBy(() -> ProductionGuards.requireExplicitHost("RABBITMQ_HOST", ""))
                .hasMessageContaining("explicit hostname");
        assertThatThrownBy(() -> ProductionGuards.requireSecret("REDIS_PASSWORD", " "))
                .hasMessageContaining("REDIS_PASSWORD");
        assertThatThrownBy(() -> ProductionGuards.requireTlsJdbcUrl("jdbc:postgresql://db/assessflow"))
                .hasMessageContaining("TLS");
        assertThatCode(() -> ProductionGuards.requireTlsJdbcUrl(
                "jdbc:postgresql://ep.neon.tech/assessflow?sslmode=require")).doesNotThrowAnyException();
        assertThatThrownBy(() -> ProductionGuards.requirePublicOrigin("APP_CORS_ALLOWED_ORIGINS", "*"))
                .hasMessageContaining("wildcard");
        assertThatThrownBy(() -> ProductionGuards.requireScaledPair(2, false, true))
                .hasMessageContaining("APP_REPLICAS>=2");
        assertThatCode(() -> ProductionGuards.requireScaledPair(2, true, true)).doesNotThrowAnyException();
        assertThatCode(() -> ProductionGuards.requireScaledPair(1, false, false)).doesNotThrowAnyException();
    }

    @Test
    void sameSiteNoneRequiresSecure() {
        assertThatThrownBy(() -> new RefreshCookieSettings(false, "None"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secure-cookie");
        assertThatCode(() -> new RefreshCookieSettings(true, "None")).doesNotThrowAnyException();
        assertThatCode(() -> new RefreshCookieSettings(true, "Strict")).doesNotThrowAnyException();
    }
}
