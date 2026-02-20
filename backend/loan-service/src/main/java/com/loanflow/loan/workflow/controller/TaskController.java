package com.loanflow.loan.workflow.controller;

import com.loanflow.dto.common.ApiResponse;
import com.loanflow.loan.workflow.WorkflowService;
import com.loanflow.loan.workflow.dto.CompleteTaskRequest;
import com.loanflow.loan.workflow.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for workflow task management — task inbox, claim, and complete.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final WorkflowService workflowService;

    /**
     * Get task inbox — unclaimed tasks available for the current user's roles.
     */
    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<TaskResponse>> getInbox(
            Principal principal,
            @PageableDefault(size = 20) Pageable pageable) {

        List<String> roles = extractRolesFromPrincipal(principal);
        return ResponseEntity.ok(workflowService.getTasksByRoles(roles, pageable));
    }

    /**
     * Get tasks claimed by the current user.
     */
    @GetMapping("/my-tasks")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(workflowService.getTasksByAssignee(jwt.getSubject(), pageable));
    }

    /**
     * Get task details by Flowable task ID.
     */
    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getTask(taskId)));
    }

    /**
     * Get the active workflow task for a specific loan application.
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskForApplication(
            @PathVariable UUID applicationId) {
        TaskResponse task = workflowService.getActiveTaskForApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    /**
     * Claim a task for the current user.
     */
    @PostMapping("/{taskId}/claim")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> claimTask(
            @PathVariable String taskId,
            @AuthenticationPrincipal Jwt jwt) {
        workflowService.claimTask(taskId, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(null, "Task claimed successfully"));
    }

    /**
     * Release a claimed task back to the candidate group pool.
     */
    @PostMapping("/{taskId}/unclaim")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unclaimTask(@PathVariable String taskId) {
        workflowService.unclaimTask(taskId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task released"));
    }

    /**
     * Complete a task with a decision (APPROVED, REJECTED, or REFERRED).
     */
    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteTaskRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("decision", request.getDecision());
        variables.put("comments", request.getComments() != null ? request.getComments() : "");

        if (request.getApprovedAmount() != null) {
            variables.put("approvedAmount", request.getApprovedAmount());
        }
        if (request.getInterestRate() != null) {
            variables.put("interestRate", request.getInterestRate());
        }

        workflowService.completeTask(taskId, variables, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(null, "Task completed successfully"));
    }

    /**
     * Extract role names (without ROLE_ prefix) from the authenticated principal
     * to match Flowable candidateGroups naming convention.
     */
    private List<String> extractRolesFromPrincipal(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .toList();
        }
        return List.of();
    }
}
