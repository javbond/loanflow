package com.loanflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LoanFlow API Gateway - Spring Cloud Gateway
 *
 * Single entry point for all microservices on port 8080.
 * Provides:
 * - Route management to all downstream services
 * - JWT token relay (Keycloak tokens forwarded to backends)
 * - Centralized CORS configuration
 * - Health-check aggregation via Actuator
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
