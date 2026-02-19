package com.loanflow.auth.service;

import com.loanflow.auth.domain.entity.Role;
import com.loanflow.auth.domain.entity.User;
import com.loanflow.auth.domain.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT Service Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set test values
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);

        // Create test user
        Role adminRole = new Role(RoleType.ADMIN);
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encoded_password")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of(adminRole))
                .build();
    }

    @Test
    @DisplayName("Should generate access token")
    void shouldGenerateAccessToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("Should generate refresh token")
    void shouldGenerateRefreshToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserId() {
        String token = jwtService.generateAccessToken(testUser);
        String userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should validate token for correct user")
    void shouldValidateToken() {
        String token = jwtService.generateAccessToken(testUser);
        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for different user")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateAccessToken(testUser);

        User differentUser = User.builder()
                .id(UUID.randomUUID())
                .email("different@example.com")
                .password("password")
                .firstName("Different")
                .lastName("User")
                .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void shouldDetectNonExpiredToken() {
        String token = jwtService.generateAccessToken(testUser);
        boolean isExpired = jwtService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should return expiration time in seconds")
    void shouldReturnExpirationTime() {
        Long expirationTime = jwtService.getExpirationTime();

        assertThat(expirationTime).isEqualTo(3600L); // 1 hour
    }
}
