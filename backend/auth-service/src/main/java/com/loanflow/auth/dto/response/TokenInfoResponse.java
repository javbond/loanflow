package com.loanflow.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Token Info Response DTO
 * Contains user information extracted from Keycloak JWT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfoResponse {

    private String subject;
    private String email;
    private Boolean emailVerified;
    private String preferredUsername;
    private String givenName;
    private String familyName;
    private String fullName;
    private Set<String> roles;
    private Instant issuedAt;
    private Instant expiresAt;
    private String issuer;
}
