package com.loanflow.policy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loanflow.dto.request.PolicyRequest;
import com.loanflow.dto.response.PolicyResponse;
import com.loanflow.policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for PolicyController.
 * Uses @AutoConfigureMockMvc(addFilters = false) to bypass security filter chain
 * (which would require a live Keycloak issuer-uri). Security role tests use
 * Spring Security Test's jwt() post-processor.
 */
@WebMvcTest(value = PolicyController.class,
        excludeAutoConfiguration = {
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class,
                MongoRepositoriesAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PolicyController Tests")
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private ObjectMapper objectMapper;
    private PolicyResponse sampleResponse;
    private PolicyRequest sampleRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = PolicyResponse.builder()
                .id("policy-1")
                .policyCode("POL-2026-000001")
                .name("Personal Loan Eligibility")
                .category("ELIGIBILITY")
                .loanType("PERSONAL_LOAN")
                .status("DRAFT")
                .versionNumber(1)
                .ruleCount(1)
                .build();

        sampleRequest = PolicyRequest.builder()
                .name("Personal Loan Eligibility")
                .description("Eligibility rules")
                .category("ELIGIBILITY")
                .loanType("PERSONAL_LOAN")
                .rules(List.of(
                        PolicyRequest.PolicyRuleRequest.builder()
                                .name("Age Check")
                                .logicalOperator("AND")
                                .conditions(List.of(
                                        PolicyRequest.ConditionRequest.builder()
                                                .field("applicant.age")
                                                .operator("GREATER_THAN_OR_EQUAL")
                                                .value("21")
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

    @Nested
    @DisplayName("POST /api/v1/policies")
    class CreatePolicy {

        @Test
        @DisplayName("should create policy successfully")
        void shouldCreatePolicy() throws Exception {
            when(policyService.create(any(PolicyRequest.class), anyString())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/policies")
                            .with(jwt().jwt(j -> j
                                    .claim("preferred_username", "admin@loanflow.com")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Personal Loan Eligibility"))
                    .andExpect(jsonPath("$.data.policyCode").value("POL-2026-000001"));
        }

        @Test
        @DisplayName("should reject validation errors (blank name, null fields)")
        void shouldRejectValidationErrors() throws Exception {
            PolicyRequest invalidRequest = PolicyRequest.builder()
                    .name("")  // blank name
                    .category(null)  // null category
                    .loanType(null)  // null loan type
                    .build();

            mockMvc.perform(post("/api/v1/policies")
                            .with(jwt().jwt(j -> j
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/policies/{id}")
    class GetPolicy {

        @Test
        @DisplayName("should get policy by id")
        void shouldGetPolicyById() throws Exception {
            when(policyService.getById("policy-1")).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/policies/policy-1")
                            .with(jwt().jwt(j -> j
                                    .claim("realm_access", Map.of("roles", List.of("LOAN_OFFICER"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value("policy-1"))
                    .andExpect(jsonPath("$.data.name").value("Personal Loan Eligibility"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/policies")
    class ListPolicies {

        @Test
        @DisplayName("should list all policies with pagination")
        void shouldListAllPolicies() throws Exception {
            Page<PolicyResponse> page = new PageImpl<>(
                    List.of(sampleResponse),
                    PageRequest.of(0, 20),
                    1);
            when(policyService.listAll(any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/policies")
                            .with(jwt().jwt(j -> j
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/policies/{id}")
    class UpdatePolicy {

        @Test
        @DisplayName("should update policy")
        void shouldUpdatePolicy() throws Exception {
            when(policyService.update(eq("policy-1"), any(PolicyRequest.class), anyString()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/policies/policy-1")
                            .with(jwt().jwt(j -> j
                                    .claim("preferred_username", "admin@loanflow.com")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(policyService).update(eq("policy-1"), any(PolicyRequest.class), anyString());
        }
    }

    @Nested
    @DisplayName("Policy Lifecycle Endpoints")
    class PolicyLifecycle {

        @Test
        @DisplayName("PATCH /activate - should activate policy")
        void shouldActivatePolicy() throws Exception {
            sampleResponse.setStatus("ACTIVE");
            when(policyService.activate(eq("policy-1"), anyString())).thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/policies/policy-1/activate")
                            .with(jwt().jwt(j -> j
                                    .claim("preferred_username", "admin@loanflow.com")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("PATCH /deactivate - should deactivate policy")
        void shouldDeactivatePolicy() throws Exception {
            sampleResponse.setStatus("INACTIVE");
            when(policyService.deactivate(eq("policy-1"), anyString())).thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/policies/policy-1/deactivate")
                            .with(jwt().jwt(j -> j
                                    .claim("preferred_username", "admin@loanflow.com")
                                    .claim("realm_access", Map.of("roles", List.of("SUPERVISOR"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/policies/{id}")
    class DeletePolicy {

        @Test
        @DisplayName("should delete policy as ADMIN")
        void shouldDeletePolicy() throws Exception {
            mockMvc.perform(delete("/api/v1/policies/policy-1")
                            .with(jwt().jwt(j -> j
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(policyService).delete("policy-1");
        }
    }

    @Nested
    @DisplayName("Search & Query Endpoints")
    class SearchAndQuery {

        @Test
        @DisplayName("GET /search - should search policies by keyword")
        void shouldSearchPolicies() throws Exception {
            Page<PolicyResponse> page = new PageImpl<>(
                    List.of(sampleResponse),
                    PageRequest.of(0, 20),
                    1);
            when(policyService.search(eq("eligibility"), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/policies/search")
                            .param("q", "eligibility")
                            .with(jwt().jwt(j -> j
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /active/{loanType} - should get active policies for loan type")
        void shouldGetActivePolicies() throws Exception {
            when(policyService.getActivePoliciesForLoanType(any())).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/policies/active/PERSONAL_LOAN")
                            .with(jwt().jwt(j -> j
                                    .claim("realm_access", Map.of("roles", List.of("UNDERWRITER"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].name").value("Personal Loan Eligibility"));
        }
    }

    @Nested
    @DisplayName("Versioning Endpoints")
    class Versioning {

        @Test
        @DisplayName("POST /versions - should create new version")
        void shouldCreateNewVersion() throws Exception {
            sampleResponse.setVersionNumber(2);
            when(policyService.createNewVersion(eq("policy-1"), anyString())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/policies/policy-1/versions")
                            .with(jwt().jwt(j -> j
                                    .claim("preferred_username", "admin@loanflow.com")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.versionNumber").value(2));
        }
    }
}
