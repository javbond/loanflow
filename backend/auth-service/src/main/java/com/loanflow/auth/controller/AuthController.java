package com.loanflow.auth.controller;

import com.loanflow.auth.dto.request.LoginRequest;
import com.loanflow.auth.dto.response.AuthResponse;
import com.loanflow.auth.dto.response.TokenInfoResponse;
import com.loanflow.auth.service.KeycloakAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller - Keycloak OAuth2/OIDC Integration
 * PRD Compliant: Uses Keycloak for authentication
 *
 * Note: User registration and password management are handled by Keycloak directly.
 * This controller provides:
 * - Token exchange via Keycloak
 * - Token info extraction
 * - Logout coordination
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Keycloak OAuth2/OIDC Authentication APIs")
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;

    @PostMapping("/login")
    @Operation(summary = "Login via Keycloak and get OAuth2 tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Keycloak login request for: {}", request.getEmail());
        AuthResponse response = keycloakAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using Keycloak refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthResponse response = keycloakAuthService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token-info")
    @Operation(summary = "Get current token info (user details from JWT)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<TokenInfoResponse> getTokenInfo(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        TokenInfoResponse response = keycloakAuthService.extractTokenInfo(jwt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user from Keycloak")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<TokenInfoResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        TokenInfoResponse response = keycloakAuthService.extractTokenInfo(jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout from Keycloak (invalidate session)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) Map<String, String> request) {

        String refreshToken = request != null ? request.get("refreshToken") : null;

        if (refreshToken != null && !refreshToken.isBlank()) {
            keycloakAuthService.logout(refreshToken);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "keycloakLogoutUrl", keycloakAuthService.getKeycloakLogoutUrl()
        ));
    }
}
