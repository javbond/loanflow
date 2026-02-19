package com.loanflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.auth.dto.request.LoginRequest;
import com.loanflow.auth.dto.response.AuthResponse;
import com.loanflow.auth.dto.response.TokenInfoResponse;
import com.loanflow.auth.service.KeycloakAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests - Keycloak OAuth2/OIDC")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakAuthService keycloakAuthService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private AuthResponse mockAuthResponse;
    private TokenInfoResponse mockTokenInfo;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .accessToken("keycloak_access_token")
                .refreshToken("keycloak_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .refreshExpiresIn(604800L)
                .scope("openid profile email")
                .build();

        mockTokenInfo = TokenInfoResponse.builder()
                .subject("user-uuid")
                .email("test@example.com")
                .emailVerified(true)
                .preferredUsername("test@example.com")
                .givenName("Test")
                .familyName("User")
                .fullName("Test User")
                .roles(Set.of("LOAN_OFFICER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("http://localhost:8180/realms/loanflow")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success via Keycloak")
    void shouldLoginSuccessfullyViaKeycloak() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        when(keycloakAuthService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("keycloak_access_token"))
                .andExpect(jsonPath("$.refreshToken").value("keycloak_refresh_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.scope").value("openid profile email"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid Credentials")
    void shouldRejectInvalidLoginViaKeycloak() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong_password")
                .build();

        when(keycloakAuthService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Success")
    void shouldRefreshTokenSuccessfully() throws Exception {
        when(keycloakAuthService.refreshToken("valid_refresh_token")).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"valid_refresh_token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("keycloak_access_token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Missing Token")
    void shouldRejectMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/auth/token-info - Returns 401 without JWT")
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        // Without a valid JWT in the request, the endpoint should return 401
        // Note: Security filters are disabled but @AuthenticationPrincipal returns null
        mockMvc.perform(get("/api/v1/auth/token-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Success")
    void shouldLogoutSuccessfully() throws Exception {
        when(keycloakAuthService.getKeycloakLogoutUrl())
                .thenReturn("http://localhost:8180/realms/loanflow/protocol/openid-connect/logout");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt().jwt(jwt -> jwt.subject("user-uuid")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"test_refresh_token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(jsonPath("$.keycloakLogoutUrl").exists());
    }
}
