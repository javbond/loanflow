package com.loanflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reactive security configuration for the API Gateway.
 *
 * The gateway validates JWT tokens issued by Keycloak and extracts
 * realm_access.roles for authorization. It then forwards the original
 * JWT to downstream services (token relay pattern).
 *
 * Authorization is mostly delegated to downstream services;
 * the gateway only enforces that API endpoints require authentication,
 * while health/actuator endpoints remain public.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers(
                                "/actuator/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout"
                        ).permitAll()
                        // All other API requests require authentication
                        .pathMatchers("/api/**").authenticated()
                        // Anything else is permitted (favicon, etc.)
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    /**
     * Converts Keycloak JWT realm_access.roles into Spring Security authorities.
     * Mirrors the logic in common-security SecurityConfig but for reactive stack.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Extract from realm_access.roles (Keycloak standard)
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof Collection<?> roles) {
                    authorities.addAll(roles.stream()
                            .filter(String.class::isInstance)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList()));
                }
            }

            // Fallback: top-level "roles" claim
            Object topLevelRoles = jwt.getClaim("roles");
            if (topLevelRoles instanceof Collection<?> roles) {
                authorities.addAll(roles.stream()
                        .filter(String.class::isInstance)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));
            }

            return authorities;
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
