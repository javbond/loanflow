package com.loanflow.auth.service;

import com.loanflow.auth.domain.entity.Role;
import com.loanflow.auth.domain.entity.User;
import com.loanflow.auth.domain.enums.RoleType;
import com.loanflow.auth.dto.request.LoginRequest;
import com.loanflow.auth.dto.request.RegisterRequest;
import com.loanflow.auth.dto.response.AuthResponse;
import com.loanflow.auth.repository.RoleRepository;
import com.loanflow.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role(RoleType.CUSTOMER);
        customerRole.setId(UUID.randomUUID());

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encoded_password")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of(customerRole))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123")
                .firstName("New")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getUser().getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getUser().getRoles()).contains("CUSTOMER");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration with existing email")
    void shouldRejectRegistrationWithExistingEmail() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password123")
                .firstName("Existing")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong_password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshToken() {
        // Given
        String refreshToken = "valid_refresh_token";

        when(jwtService.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenExpired(refreshToken)).thenReturn(false);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("new_refresh_token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        // When
        AuthResponse response = authService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() {
        // Given
        String expiredToken = "expired_token";

        when(jwtService.extractUsername(expiredToken)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenExpired(expiredToken)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(expiredToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token expired");
    }
}
