package com.loanflow.auth.service;

import com.loanflow.auth.config.KeycloakProperties;
import com.loanflow.auth.dto.response.TokenInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Keycloak Auth Service Tests")
class KeycloakAuthServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private KeycloakProperties keycloakProperties;

    @Mock
    private Jwt jwt;

    private KeycloakAuthService keycloakAuthService;

    @BeforeEach
    void setUp() {
        keycloakAuthService = new KeycloakAuthService(webClient, keycloakProperties);
    }

    @Test
    @DisplayName("Should extract token info from JWT - realm_access roles")
    void shouldExtractTokenInfoFromJwt() {
        // Given
        when(jwt.getSubject()).thenReturn("user-uuid-123");
        when(jwt.getClaimAsString("email")).thenReturn("officer@loanflow.com");
        when(jwt.getClaim("email_verified")).thenReturn(true);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("officer@loanflow.com");
        when(jwt.getClaimAsString("given_name")).thenReturn("Loan");
        when(jwt.getClaimAsString("family_name")).thenReturn("Officer");
        when(jwt.getClaimAsString("name")).thenReturn("Loan Officer");
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        try {
            when(jwt.getIssuer()).thenReturn(new java.net.URL("http://localhost:8180/realms/loanflow"));
        } catch (Exception ignored) {}

        // Keycloak stores roles in realm_access.roles
        Map<String, Object> realmAccess = Map.of("roles", List.of("LOAN_OFFICER", "default-roles-loanflow"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
        when(jwt.getClaim("roles")).thenReturn(null);

        // When
        TokenInfoResponse response = keycloakAuthService.extractTokenInfo(jwt);

        // Then
        assertThat(response.getSubject()).isEqualTo("user-uuid-123");
        assertThat(response.getEmail()).isEqualTo("officer@loanflow.com");
        assertThat(response.getEmailVerified()).isTrue();
        assertThat(response.getGivenName()).isEqualTo("Loan");
        assertThat(response.getFamilyName()).isEqualTo("Officer");
        assertThat(response.getFullName()).isEqualTo("Loan Officer");
        assertThat(response.getRoles()).containsExactly("LOAN_OFFICER");
        assertThat(response.getRoles()).doesNotContain("default-roles-loanflow");
        assertThat(response.getIssuer()).isEqualTo("http://localhost:8180/realms/loanflow");
    }

    @Test
    @DisplayName("Should extract roles from custom roles claim")
    void shouldExtractRolesFromCustomClaim() {
        // Given
        when(jwt.getSubject()).thenReturn("user-uuid-456");
        when(jwt.getClaim("realm_access")).thenReturn(null);
        when(jwt.getClaim("roles")).thenReturn(List.of("UNDERWRITER", "ADMIN"));
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));

        // When
        TokenInfoResponse response = keycloakAuthService.extractTokenInfo(jwt);

        // Then
        assertThat(response.getRoles()).containsExactlyInAnyOrder("UNDERWRITER", "ADMIN");
    }

    @Test
    @DisplayName("Should return empty roles when no roles present")
    void shouldReturnEmptyRolesWhenNonePresent() {
        // Given
        when(jwt.getSubject()).thenReturn("user-uuid-789");
        when(jwt.getClaim("realm_access")).thenReturn(null);
        when(jwt.getClaim("roles")).thenReturn(null);
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));

        // When
        TokenInfoResponse response = keycloakAuthService.extractTokenInfo(jwt);

        // Then
        assertThat(response.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("Should get Keycloak logout URL")
    void shouldGetKeycloakLogoutUrl() {
        // Given
        when(keycloakProperties.getEndSessionUri())
                .thenReturn("http://localhost:8180/realms/loanflow/protocol/openid-connect/logout");

        // When
        String logoutUrl = keycloakAuthService.getKeycloakLogoutUrl();

        // Then
        assertThat(logoutUrl).isEqualTo("http://localhost:8180/realms/loanflow/protocol/openid-connect/logout");
    }
}
