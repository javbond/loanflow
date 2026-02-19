package com.loanflow.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keycloak OAuth2 Token Response DTO
 * PRD Compliant: Returns Keycloak tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long refreshExpiresIn;
    private String scope;
}
