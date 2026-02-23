package com.loanflow.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route definitions for Spring Cloud Gateway.
 *
 * Each route maps a path prefix to a downstream microservice.
 * The JWT token is automatically forwarded via the TokenRelay filter
 * configured per route. Circuit breaker fallbacks return 503 when
 * a downstream service is unreachable.
 *
 * Route mapping:
 *   /api/v1/auth/**              -> auth-service     (8085)
 *   /api/v1/customers/**         -> customer-service  (8082)
 *   /api/v1/loans/**             -> loan-service      (8081)
 *   /api/v1/tasks/**             -> loan-service      (8081)
 *   /api/v1/risk/**              -> loan-service      (8081)
 *   /api/v1/decisions/**         -> loan-service      (8081)
 *   /api/v1/approval-hierarchy/**-> loan-service      (8081)
 *   /api/v1/documents/**         -> document-service   (8083)
 *   /api/v1/notifications/**     -> notification-service(8084)
 *   /api/v1/policies/**          -> policy-service     (8086)
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ─── Auth Service (8085) ─────────────────────────────
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://localhost:8085"))

                // ─── Customer Service (8082) ─────────────────────────
                .route("customer-service", r -> r
                        .path("/api/v1/customers/**")
                        .uri("http://localhost:8082"))

                // ─── Loan Service (8081) — multiple path prefixes ────
                .route("loan-service-loans", r -> r
                        .path("/api/v1/loans/**")
                        .uri("http://localhost:8081"))

                .route("loan-service-tasks", r -> r
                        .path("/api/v1/tasks/**")
                        .uri("http://localhost:8081"))

                .route("loan-service-risk", r -> r
                        .path("/api/v1/risk/**")
                        .uri("http://localhost:8081"))

                .route("loan-service-decisions", r -> r
                        .path("/api/v1/decisions/**")
                        .uri("http://localhost:8081"))

                .route("loan-service-approval-hierarchy", r -> r
                        .path("/api/v1/approval-hierarchy/**")
                        .uri("http://localhost:8081"))

                // ─── Document Service (8083) ─────────────────────────
                .route("document-service", r -> r
                        .path("/api/v1/documents/**")
                        .uri("http://localhost:8083"))

                // ─── Notification Service (8084) ─────────────────────
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .uri("http://localhost:8084"))

                // ─── Policy Service (8086) ───────────────────────────
                .route("policy-service", r -> r
                        .path("/api/v1/policies/**")
                        .uri("http://localhost:8086"))

                .build();
    }
}
