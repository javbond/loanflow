package com.loanflow.auth.service;

import com.loanflow.auth.domain.entity.Role;
import com.loanflow.auth.domain.entity.User;
import com.loanflow.auth.domain.enums.RoleType;
import com.loanflow.auth.dto.response.UserResponse;
import com.loanflow.auth.repository.RoleRepository;
import com.loanflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User management service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * Get all users with pagination
     */
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return mapToUserResponse(user);
    }

    /**
     * Assign role to user
     */
    @Transactional
    public UserResponse assignRole(UUID userId, RoleType roleType) {
        log.info("Assigning role {} to user {}", roleType, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleType));

        user.addRole(role);
        User savedUser = userRepository.save(user);

        log.info("Role {} assigned to user {}", roleType, userId);
        return mapToUserResponse(savedUser);
    }

    /**
     * Remove role from user
     */
    @Transactional
    public UserResponse removeRole(UUID userId, RoleType roleType) {
        log.info("Removing role {} from user {}", roleType, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleType));

        user.removeRole(role);
        User savedUser = userRepository.save(user);

        log.info("Role {} removed from user {}", roleType, userId);
        return mapToUserResponse(savedUser);
    }

    /**
     * Enable/disable user
     */
    @Transactional
    public UserResponse setUserEnabled(UUID userId, boolean enabled) {
        log.info("Setting user {} enabled: {}", userId, enabled);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
