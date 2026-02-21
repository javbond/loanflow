package com.loanflow.policy.service;

import com.loanflow.dto.request.PolicyRequest;
import com.loanflow.dto.response.PolicyResponse;
import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import com.loanflow.policy.mapper.PolicyMapper;
import com.loanflow.policy.repository.PolicyRepository;
import com.loanflow.policy.service.impl.PolicyServiceImpl;
import com.loanflow.util.exception.BusinessException;
import com.loanflow.util.exception.DuplicateResourceException;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PolicyServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService Tests")
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyMapper policyMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PolicyServiceImpl policyService;

    private Policy samplePolicy;
    private PolicyRequest sampleRequest;
    private PolicyResponse sampleResponse;

    @BeforeEach
    void setUp() {
        samplePolicy = createSamplePolicy();
        sampleRequest = createSampleRequest();
        sampleResponse = createSampleResponse();
    }

    @Nested
    @DisplayName("Create Policy")
    class CreatePolicy {

        @Test
        @DisplayName("Should create policy successfully")
        void shouldCreatePolicy() {
            when(policyRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(policyMapper.toEntity(any(PolicyRequest.class))).thenReturn(samplePolicy);
            when(policyRepository.save(any(Policy.class))).thenReturn(samplePolicy);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);

            PolicyResponse result = policyService.create(sampleRequest, "admin@loanflow.com");

            assertNotNull(result);
            assertEquals("Personal Loan Eligibility", result.getName());
            verify(policyRepository).save(any(Policy.class));
        }

        @Test
        @DisplayName("Should throw when duplicate name")
        void shouldThrowWhenDuplicateName() {
            when(policyRepository.existsByNameIgnoreCase(anyString())).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> policyService.create(sampleRequest, "admin@loanflow.com"));

            verify(policyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Policy")
    class GetPolicy {

        @Test
        @DisplayName("Should get policy by ID with cache miss")
        void shouldGetPolicyByIdCacheMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));
            when(policyMapper.toResponse(samplePolicy)).thenReturn(sampleResponse);

            PolicyResponse result = policyService.getById("policy-1");

            assertNotNull(result);
            assertEquals("Personal Loan Eligibility", result.getName());
            verify(policyRepository).findById("policy-1");
        }

        @Test
        @DisplayName("Should get policy by ID from cache")
        void shouldGetPolicyByIdFromCache() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(sampleResponse);

            PolicyResponse result = policyService.getById("policy-1");

            assertNotNull(result);
            verify(policyRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should throw when policy not found")
        void shouldThrowWhenNotFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(policyRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> policyService.getById("nonexistent"));
        }

        @Test
        @DisplayName("Should get policy by code")
        void shouldGetPolicyByCode() {
            when(policyRepository.findFirstByPolicyCodeOrderByVersionNumberDesc("POL-2026-000001"))
                    .thenReturn(Optional.of(samplePolicy));
            when(policyMapper.toResponse(samplePolicy)).thenReturn(sampleResponse);

            PolicyResponse result = policyService.getByPolicyCode("POL-2026-000001");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should get version history")
        void shouldGetVersionHistory() {
            Policy v1 = createSamplePolicy();
            v1.setVersionNumber(1);
            Policy v2 = createSamplePolicy();
            v2.setVersionNumber(2);

            when(policyRepository.findByPolicyCodeOrderByVersionNumberDesc("POL-2026-000001"))
                    .thenReturn(List.of(v2, v1));
            when(policyMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponse, sampleResponse));

            List<PolicyResponse> result = policyService.getVersionHistory("POL-2026-000001");
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("Update Policy")
    class UpdatePolicy {

        @Test
        @DisplayName("Should update DRAFT policy")
        void shouldUpdateDraftPolicy() {
            samplePolicy.setStatus(PolicyStatus.DRAFT);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));
            when(policyRepository.save(any(Policy.class))).thenReturn(samplePolicy);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            PolicyResponse result = policyService.update("policy-1", sampleRequest, "admin");
            assertNotNull(result);
            verify(policyMapper).updateEntity(any(Policy.class), any(PolicyRequest.class));
        }

        @Test
        @DisplayName("Should throw when updating ACTIVE policy")
        void shouldThrowWhenUpdatingActivePolicy() {
            samplePolicy.setStatus(PolicyStatus.ACTIVE);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));

            assertThrows(BusinessException.class,
                    () -> policyService.update("policy-1", sampleRequest, "admin"));
        }

        @Test
        @DisplayName("Should throw when updating ARCHIVED policy")
        void shouldThrowWhenUpdatingArchivedPolicy() {
            samplePolicy.setStatus(PolicyStatus.ARCHIVED);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));

            assertThrows(BusinessException.class,
                    () -> policyService.update("policy-1", sampleRequest, "admin"));
        }
    }

    @Nested
    @DisplayName("Policy Lifecycle")
    class PolicyLifecycle {

        @Test
        @DisplayName("Should activate policy")
        void shouldActivatePolicy() {
            samplePolicy.setStatus(PolicyStatus.DRAFT);
            samplePolicy.setLoanType(LoanType.PERSONAL_LOAN);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));
            when(policyRepository.save(any(Policy.class))).thenReturn(samplePolicy);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            PolicyResponse result = policyService.activate("policy-1", "admin");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should deactivate policy")
        void shouldDeactivatePolicy() {
            samplePolicy.setStatus(PolicyStatus.ACTIVE);
            samplePolicy.setLoanType(LoanType.PERSONAL_LOAN);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));
            when(policyRepository.save(any(Policy.class))).thenReturn(samplePolicy);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            PolicyResponse result = policyService.deactivate("policy-1", "admin");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should create new version")
        void shouldCreateNewVersion() {
            samplePolicy.setId("original-id");
            samplePolicy.setStatus(PolicyStatus.ACTIVE);
            samplePolicy.setLoanType(LoanType.PERSONAL_LOAN);

            Policy newVersionPolicy = createSamplePolicy();
            newVersionPolicy.setVersionNumber(2);

            when(policyRepository.findById("original-id")).thenReturn(Optional.of(samplePolicy));
            when(policyRepository.save(any(Policy.class))).thenReturn(samplePolicy).thenReturn(newVersionPolicy);
            when(policyMapper.toResponse(newVersionPolicy)).thenReturn(sampleResponse);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            PolicyResponse result = policyService.createNewVersion("original-id", "admin");
            assertNotNull(result);
            verify(policyRepository, times(2)).save(any(Policy.class));
        }
    }

    @Nested
    @DisplayName("Delete Policy")
    class DeletePolicy {

        @Test
        @DisplayName("Should delete DRAFT policy")
        void shouldDeleteDraftPolicy() {
            samplePolicy.setStatus(PolicyStatus.DRAFT);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));
            when(redisTemplate.delete(anyString())).thenReturn(true);

            assertDoesNotThrow(() -> policyService.delete("policy-1"));
            verify(policyRepository).delete(samplePolicy);
        }

        @Test
        @DisplayName("Should throw when deleting non-DRAFT policy")
        void shouldThrowWhenDeletingNonDraftPolicy() {
            samplePolicy.setStatus(PolicyStatus.ACTIVE);
            when(policyRepository.findById("policy-1")).thenReturn(Optional.of(samplePolicy));

            assertThrows(BusinessException.class, () -> policyService.delete("policy-1"));
            verify(policyRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @Test
        @DisplayName("Should list all policies with pagination")
        void shouldListAllPolicies() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Policy> page = new PageImpl<>(List.of(samplePolicy), pageable, 1);
            when(policyRepository.findAll(pageable)).thenReturn(page);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);

            Page<PolicyResponse> result = policyService.listAll(pageable);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should list policies by category")
        void shouldListByCategory() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Policy> page = new PageImpl<>(List.of(samplePolicy), pageable, 1);
            when(policyRepository.findByCategory(PolicyCategory.ELIGIBILITY, pageable)).thenReturn(page);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);

            Page<PolicyResponse> result = policyService.listByCategory(PolicyCategory.ELIGIBILITY, pageable);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should search policies by text")
        void shouldSearchByText() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Policy> page = new PageImpl<>(List.of(samplePolicy), pageable, 1);
            when(policyRepository.searchByText("eligibility", pageable)).thenReturn(page);
            when(policyMapper.toResponse(any(Policy.class))).thenReturn(sampleResponse);

            Page<PolicyResponse> result = policyService.search("eligibility", pageable);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should get active policies for loan type")
        void shouldGetActivePoliciesForLoanType() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(policyRepository.findActivePoliciesForLoanType(LoanType.PERSONAL_LOAN))
                    .thenReturn(List.of(samplePolicy));
            when(policyMapper.toResponseList(anyList())).thenReturn(List.of(sampleResponse));

            List<PolicyResponse> result = policyService.getActivePoliciesForLoanType(LoanType.PERSONAL_LOAN);
            assertEquals(1, result.size());
        }
    }

    // ==================== Helper Methods ====================

    private Policy createSamplePolicy() {
        return Policy.builder()
                .id("policy-1")
                .policyCode("POL-2026-000001")
                .name("Personal Loan Eligibility")
                .description("Eligibility rules for personal loans")
                .category(PolicyCategory.ELIGIBILITY)
                .loanType(LoanType.PERSONAL_LOAN)
                .status(PolicyStatus.DRAFT)
                .versionNumber(1)
                .priority(100)
                .rules(new ArrayList<>(List.of(
                        PolicyRule.builder()
                                .name("Age Check")
                                .logicalOperator(LogicalOperator.AND)
                                .conditions(List.of(
                                        Condition.builder()
                                                .field("applicant.age")
                                                .operator(ConditionOperator.BETWEEN)
                                                .minValue("21")
                                                .maxValue("58")
                                                .build()
                                ))
                                .actions(List.of(
                                        Action.builder()
                                                .type(ActionType.APPROVE)
                                                .parameters(Map.of())
                                                .build()
                                ))
                                .priority(100)
                                .enabled(true)
                                .build()
                )))
                .tags(new ArrayList<>(List.of("personal-loan", "eligibility")))
                .createdBy("admin@loanflow.com")
                .build();
    }

    private PolicyRequest createSampleRequest() {
        return PolicyRequest.builder()
                .name("Personal Loan Eligibility")
                .description("Eligibility rules for personal loans")
                .category("ELIGIBILITY")
                .loanType("PERSONAL_LOAN")
                .priority(100)
                .tags(List.of("personal-loan", "eligibility"))
                .rules(List.of(
                        PolicyRequest.PolicyRuleRequest.builder()
                                .name("Age Check")
                                .logicalOperator("AND")
                                .conditions(List.of(
                                        PolicyRequest.ConditionRequest.builder()
                                                .field("applicant.age")
                                                .operator("BETWEEN")
                                                .minValue("21")
                                                .maxValue("58")
                                                .build()
                                ))
                                .actions(List.of(
                                        PolicyRequest.ActionRequest.builder()
                                                .type("APPROVE")
                                                .parameters(Map.of())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private PolicyResponse createSampleResponse() {
        return PolicyResponse.builder()
                .id("policy-1")
                .policyCode("POL-2026-000001")
                .name("Personal Loan Eligibility")
                .category("ELIGIBILITY")
                .loanType("PERSONAL_LOAN")
                .status("DRAFT")
                .versionNumber(1)
                .ruleCount(1)
                .build();
    }
}
