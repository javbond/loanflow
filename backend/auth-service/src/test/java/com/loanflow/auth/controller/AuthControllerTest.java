package com.loanflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.auth.dto.request.LoginRequest;
import com.loanflow.auth.dto.request.RegisterRequest;
import com.loanflow.auth.dto.response.AuthResponse;
import com.loanflow.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserInfo.builder()
                        .id("user-id")
                        .email("test@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .fullName("Test User")
                        .roles(Set.of("CUSTOMER"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Success")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123")
                .firstName("New")
                .lastName("User")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test_access_token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Validation Error")
    void shouldRejectInvalidRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email")
                .password("short")
                .firstName("")
                .lastName("")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test_access_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid Credentials")
    void shouldRejectInvalidLogin() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong_password")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Success")
    void shouldRefreshTokenSuccessfully() throws Exception {
        when(authService.refreshToken("valid_refresh_token")).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"valid_refresh_token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test_access_token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Success")
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
