package com.davigama.assessflow.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {
    @Bean
    OpenAPI assessFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AssessFlow API")
                        .version("1.0.0")
                        .description("""
                                Multi-tenant assessment platform. Hosts authenticate with an opaque Bearer access token \
                                and an HttpOnly refresh cookie (`assessflow_refresh`, path `/api/v1/auth`). \
                                Guests join live sessions with a participant Bearer token from POST /api/v1/live-sessions/join. \
                                STOMP is documented in docs/websocket.md, not in this OpenAPI document.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearer-access", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("opaque")
                                .description("User access token from login/register/refresh."))
                        .addSecuritySchemes("bearer-participant", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("opaque")
                                .description("Guest participant token from live-session join.")));
    }
}
