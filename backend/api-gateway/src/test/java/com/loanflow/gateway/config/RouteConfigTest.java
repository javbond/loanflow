package com.loanflow.gateway.config;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for API Gateway route configuration.
 * Verifies that all expected routes are registered and map to correct services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RouteConfigTest.MockJwtConfig.class)
class RouteConfigTest {

    @TestConfiguration
    static class MockJwtConfig {
        @Bean
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.error(new RuntimeException("Mock JWT decoder — Keycloak not available in tests"));
        }
    }

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    @DisplayName("Route Registration")
    class RouteRegistration {

        @Test
        @DisplayName("Should register all expected service routes")
        void shouldRegisterAllRoutes() {
            Flux<Route> routes = routeLocator.getRoutes();
            List<String> routeIds = routes.map(Route::getId).collectList().block();

            assertThat(routeIds).isNotNull();
            assertThat(routeIds).contains(
                    "auth-service",
                    "customer-service",
                    "loan-service-loans",
                    "loan-service-tasks",
                    "loan-service-risk",
                    "loan-service-decisions",
                    "loan-service-approval-hierarchy",
                    "document-service",
                    "notification-service",
                    "policy-service"
            );
        }

        @Test
        @DisplayName("Should have at least 10 routes configured")
        void shouldHaveMinimumRoutes() {
            List<Route> routes = routeLocator.getRoutes().collectList().block();

            assertThat(routes).isNotNull();
            assertThat(routes.size()).isGreaterThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Route URI Mapping")
    class RouteUriMapping {

        @Test
        @DisplayName("Auth service route should map to port 8085")
        void authServiceRoute() {
            Route route = findRouteById("auth-service");

            assertThat(route).isNotNull();
            assertThat(route.getUri().toString()).contains("8085");
        }

        @Test
        @DisplayName("Customer service route should map to port 8082")
        void customerServiceRoute() {
            Route route = findRouteById("customer-service");

            assertThat(route).isNotNull();
            assertThat(route.getUri().toString()).contains("8082");
        }

        @Test
        @DisplayName("Loan service routes should map to port 8081")
        void loanServiceRoute() {
            Route route = findRouteById("loan-service-loans");

            assertThat(route).isNotNull();
            assertThat(route.getUri().toString()).contains("8081");
        }

        @Test
        @DisplayName("Document service route should map to port 8083")
        void documentServiceRoute() {
            Route route = findRouteById("document-service");

            assertThat(route).isNotNull();
            assertThat(route.getUri().toString()).contains("8083");
        }

        @Test
        @DisplayName("Notification service route should map to port 8084")
        void notificationServiceRoute() {
            Route route = findRouteById("notification-service");

            assertThat(route).isNotNull();
            assertThat(route.getUri().toString()).contains("8084");
        }

        @Test
        @DisplayName("Policy service route should map to port 8086")
        void policyServiceRoute() {
            Route route = findRouteById("policy-service");

            assertThat(route).isNotNull();
            assertThat(route.getUri().toString()).contains("8086");
        }

        private Route findRouteById(String routeId) {
            return routeLocator.getRoutes()
                    .filter(route -> route.getId().equals(routeId))
                    .blockFirst();
        }
    }

    @Nested
    @DisplayName("Actuator Endpoints")
    class ActuatorEndpoints {

        @Test
        @DisplayName("Health endpoint should be accessible without authentication")
        void healthEndpointAccessible() {
            webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Info endpoint should be accessible without authentication")
        void infoEndpointAccessible() {
            webTestClient.get()
                    .uri("/actuator/info")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("Security - Unauthenticated Requests")
    class SecurityTests {

        @Test
        @DisplayName("API requests without JWT should return 401")
        void apiRequestsRequireAuth() {
            webTestClient.get()
                    .uri("/api/v1/customers")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Auth login endpoint should not require JWT")
        void authLoginIsPublic() {
            // The gateway allows this through, but downstream may not be running (502/503).
            // We verify it doesn't get a 401 from the gateway's security filter.
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .exchange()
                    .expectStatus().value(status ->
                            assertThat(status).isNotEqualTo(401));
        }
    }
}
