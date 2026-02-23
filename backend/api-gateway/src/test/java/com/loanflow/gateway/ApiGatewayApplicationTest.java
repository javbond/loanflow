package com.loanflow.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

/**
 * Smoke test — verifies the gateway application context loads successfully.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ApiGatewayApplicationTest.MockJwtConfig.class)
class ApiGatewayApplicationTest {

    @TestConfiguration
    static class MockJwtConfig {
        @Bean
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            // Return a mock decoder that rejects all tokens (no Keycloak needed for tests)
            return token -> Mono.error(new RuntimeException("Mock JWT decoder — Keycloak not available in tests"));
        }
    }

    @Test
    @DisplayName("Gateway application context loads successfully")
    void contextLoads() {
        // Verifies all beans, routes, and config wire up correctly
    }
}
