package com.loanflow.auth.service;

import com.loanflow.auth.config.KeycloakProperties;
import com.loanflow.auth.dto.request.LoginRequest;
import com.loanflow.auth.dto.response.AuthResponse;
import com.loanflow.auth.dto.response.TokenInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Keycloak Authentication Service
 * PRD Compliant: Handles OAuth2/OIDC token exchange with Keycloak
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthService {

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;

    /**
     * Login using Keycloak's token endpoint (Resource Owner Password Credentials Grant)
     * Note: ROPC grant is used for backwards compatibility with login form.
     * For production, consider using Authorization Code Flow with PKCE.
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user {} via Keycloak", request.getEmail());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakProperties.getClientId());
        formData.add("username", request.getEmail());
        formData.add("password", request.getPassword());

        if (keycloakProperties.getClientSecret() != null && !keycloakProperties.getClientSecret().isBlank()) {
            formData.add("client_secret", keycloakProperties.getClientSecret());
        }

        try {
            KeycloakTokenResponse tokenResponse = webClient.post()
                    .uri(keycloakProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(KeycloakTokenResponse.class)
                    .block();

            if (tokenResponse == null) {
                throw new BadCredentialsException("No response from Keycloak");
            }

            return buildAuthResponse(tokenResponse);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.BadRequest e) {
            log.warn("Authentication failed for user {}: {}", request.getEmail(), e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Refresh access token using Keycloak refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token via Keycloak");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", keycloakProperties.getClientId());
        formData.add("refresh_token", refreshToken);

        if (keycloakProperties.getClientSecret() != null && !keycloakProperties.getClientSecret().isBlank()) {
            formData.add("client_secret", keycloakProperties.getClientSecret());
        }

        try {
            KeycloakTokenResponse tokenResponse = webClient.post()
                    .uri(keycloakProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(KeycloakTokenResponse.class)
                    .block();

            if (tokenResponse == null) {
                throw new IllegalArgumentException("No response from Keycloak");
            }

            return buildAuthResponse(tokenResponse);
        } catch (WebClientResponseException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
    }

    /**
     * Logout by invalidating refresh token
     */
    public void logout(String refreshToken) {
        log.info("Logging out user via Keycloak");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", keycloakProperties.getClientId());
        formData.add("refresh_token", refreshToken);

        if (keycloakProperties.getClientSecret() != null && !keycloakProperties.getClientSecret().isBlank()) {
            formData.add("client_secret", keycloakProperties.getClientSecret());
        }

        try {
            webClient.post()
                    .uri(keycloakProperties.getLogoutUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.warn("Logout request failed (token may already be expired): {}", e.getMessage());
        }
    }

    /**
     * Get Keycloak logout URL for frontend redirect
     */
    public String getKeycloakLogoutUrl() {
        return keycloakProperties.getEndSessionUri();
    }

    /**
     * Extract user info from JWT token
     */
    public TokenInfoResponse extractTokenInfo(Jwt jwt) {
        Set<String> roles = extractRoles(jwt);

        return TokenInfoResponse.builder()
                .subject(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .emailVerified(jwt.getClaim("email_verified"))
                .preferredUsername(jwt.getClaimAsString("preferred_username"))
                .givenName(jwt.getClaimAsString("given_name"))
                .familyName(jwt.getClaimAsString("family_name"))
                .fullName(jwt.getClaimAsString("name"))
                .roles(roles)
                .issuedAt(jwt.getIssuedAt())
                .expiresAt(jwt.getExpiresAt())
                .issuer(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null)
                .build();
    }

    private Set<String> extractRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // Extract from realm_access.roles (Keycloak standard)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles.stream()
                        .filter(role -> !role.startsWith("default-roles-"))
                        .collect(Collectors.toSet()));
            }
        }

        // Also check custom roles claim
        List<String> customRoles = jwt.getClaim("roles");
        if (customRoles != null) {
            roles.addAll(customRoles.stream()
                    .filter(role -> !role.startsWith("default-roles-"))
                    .collect(Collectors.toSet()));
        }

        return roles;
    }

    private AuthResponse buildAuthResponse(KeycloakTokenResponse tokenResponse) {
        return AuthResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                .scope(tokenResponse.getScope())
                .build();
    }

    /**
     * Internal class to map Keycloak token response
     */
    @lombok.Data
    private static class KeycloakTokenResponse {
        private String access_token;
        private String refresh_token;
        private String token_type;
        private Long expires_in;
        private Long refresh_expires_in;
        private String scope;

        public String getAccessToken() { return access_token; }
        public String getRefreshToken() { return refresh_token; }
        public String getTokenType() { return token_type; }
        public Long getExpiresIn() { return expires_in; }
        public Long getRefreshExpiresIn() { return refresh_expires_in; }
        public String getScope() { return scope; }
    }
}
