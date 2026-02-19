package com.loanflow.auth.controller;

import com.loanflow.auth.domain.enums.RoleType;
import com.loanflow.auth.dto.response.UserResponse;
import com.loanflow.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * User Management Controller (Admin only)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Admin APIs for user management")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users with pagination")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        String roleName = request.get("role");
        if (roleName == null || roleName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        RoleType roleType = RoleType.valueOf(roleName.toUpperCase());
        UserResponse user = userService.assignRole(id, roleType);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}/roles/{role}")
    @Operation(summary = "Remove role from user")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable UUID id,
            @PathVariable String role) {

        RoleType roleType = RoleType.valueOf(role.toUpperCase());
        UserResponse user = userService.removeRole(id, roleType);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable or disable user")
    public ResponseEntity<UserResponse> setUserEnabled(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> request) {

        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().build();
        }

        UserResponse user = userService.setUserEnabled(id, enabled);
        return ResponseEntity.ok(user);
    }
}
