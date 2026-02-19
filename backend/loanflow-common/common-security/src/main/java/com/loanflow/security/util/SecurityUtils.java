package com.loanflow.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SecurityUtils {

    /**
     * Get current authenticated user's ID (UUID from Keycloak sub claim)
     */
    public static Optional<UUID> getCurrentUserId() {
        return getJwt().map(jwt -> {
            String sub = jwt.getSubject();
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    /**
     * Get current authenticated user's username
     */
    public static Optional<String> getCurrentUsername() {
        return getJwt().map(jwt -> jwt.getClaim("preferred_username"));
    }

    /**
     * Get current authenticated user's email
     */
    public static Optional<String> getCurrentUserEmail() {
        return getJwt().map(jwt -> jwt.getClaim("email"));
    }

    /**
     * Get current authenticated user's full name
     */
    public static Optional<String> getCurrentUserFullName() {
        return getJwt().map(jwt -> jwt.getClaim("name"));
    }

    /**
     * Get all roles of current authenticated user
     */
    public static Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toSet());
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role.toUpperCase());
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        Set<String> userRoles = getCurrentUserRoles();
        for (String role : roles) {
            if (userRoles.contains(role.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has all of the specified roles
     */
    public static boolean hasAllRoles(String... roles) {
        Set<String> userRoles = getCurrentUserRoles();
        for (String role : roles) {
            if (!userRoles.contains(role.toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the branch code of current user (custom claim)
     */
    public static Optional<String> getCurrentUserBranch() {
        return getJwt().map(jwt -> jwt.getClaim("branch_code"));
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Get the raw JWT token
     */
    public static Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }
        return Optional.empty();
    }

    /**
     * Get a custom claim from JWT
     */
    public static <T> Optional<T> getClaim(String claimName, Class<T> claimType) {
        return getJwt().map(jwt -> jwt.getClaim(claimName));
    }
}
