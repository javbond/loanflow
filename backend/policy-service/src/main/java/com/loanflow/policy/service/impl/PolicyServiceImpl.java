package com.loanflow.policy.service.impl;

import com.loanflow.dto.request.PolicyRequest;
import com.loanflow.dto.response.PolicyResponse;
import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.LoanType;
import com.loanflow.policy.domain.enums.PolicyCategory;
import com.loanflow.policy.domain.enums.PolicyStatus;
import com.loanflow.policy.mapper.PolicyMapper;
import com.loanflow.policy.repository.PolicyRepository;
import com.loanflow.policy.service.PolicyService;
import com.loanflow.util.exception.BusinessException;
import com.loanflow.util.exception.DuplicateResourceException;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of PolicyService with MongoDB persistence and Redis caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "policy:";
    private static final String CACHE_ACTIVE_PREFIX = "policy:active:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    // ==================== CRUD Operations ====================

    @Override
    public PolicyResponse create(PolicyRequest request, String createdBy) {
        log.info("Creating policy: {} by {}", request.getName(), createdBy);

        // Check for duplicate name
        if (policyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Policy with name '" + request.getName() + "' already exists");
        }

        Policy policy = policyMapper.toEntity(request);
        policy.setCreatedBy(createdBy);
        policy.setModifiedBy(createdBy);

        Policy saved = policyRepository.save(policy);
        log.info("Created policy: {} (code: {})", saved.getId(), saved.getPolicyCode());

        return policyMapper.toResponse(saved);
    }

    @Override
    public PolicyResponse getById(String id) {
        // Try cache first
        String cacheKey = CACHE_PREFIX + id;
        PolicyResponse cached = getCachedResponse(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for policy: {}", id);
            return cached;
        }

        Policy policy = findPolicyById(id);
        PolicyResponse response = policyMapper.toResponse(policy);

        // Cache the result
        cacheResponse(cacheKey, response);
        return response;
    }

    @Override
    public PolicyResponse getByPolicyCode(String policyCode) {
        Policy policy = policyRepository.findFirstByPolicyCodeOrderByVersionNumberDesc(policyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with code: " + policyCode));
        return policyMapper.toResponse(policy);
    }

    @Override
    public List<PolicyResponse> getVersionHistory(String policyCode) {
        List<Policy> versions = policyRepository.findByPolicyCodeOrderByVersionNumberDesc(policyCode);
        if (versions.isEmpty()) {
            throw new ResourceNotFoundException("Policy not found with code: " + policyCode);
        }
        return policyMapper.toResponseList(versions);
    }

    @Override
    public PolicyResponse update(String id, PolicyRequest request, String modifiedBy) {
        log.info("Updating policy: {} by {}", id, modifiedBy);

        Policy policy = findPolicyById(id);

        // Only DRAFT or INACTIVE policies can be updated directly
        if (policy.getStatus() == PolicyStatus.ACTIVE || policy.getStatus() == PolicyStatus.ARCHIVED) {
            throw BusinessException.invalidOperation(
                    "Cannot update an " + policy.getStatus() + " policy. Create a new version instead.");
        }

        policyMapper.updateEntity(policy, request);
        policy.setModifiedBy(modifiedBy);

        Policy saved = policyRepository.save(policy);
        evictCache(id);

        log.info("Updated policy: {} (v{})", saved.getPolicyCode(), saved.getVersionNumber());
        return policyMapper.toResponse(saved);
    }

    @Override
    public PolicyResponse createNewVersion(String id, String createdBy) {
        log.info("Creating new version of policy: {} by {}", id, createdBy);

        Policy currentPolicy = findPolicyById(id);

        // Archive the current version if active
        if (currentPolicy.getStatus() == PolicyStatus.ACTIVE) {
            currentPolicy.archive();
            policyRepository.save(currentPolicy);
            evictCache(id);
            evictActiveCache(currentPolicy.getLoanType());
        }

        // Create new version
        Policy newVersion = currentPolicy.createNewVersion();
        newVersion.setCreatedBy(createdBy);
        newVersion.setModifiedBy(createdBy);

        Policy saved = policyRepository.save(newVersion);
        log.info("Created new version: {} v{}", saved.getPolicyCode(), saved.getVersionNumber());

        return policyMapper.toResponse(saved);
    }

    @Override
    public PolicyResponse activate(String id, String modifiedBy) {
        log.info("Activating policy: {} by {}", id, modifiedBy);

        Policy policy = findPolicyById(id);
        policy.activate(); // Throws if no rules
        policy.setModifiedBy(modifiedBy);

        Policy saved = policyRepository.save(policy);
        evictCache(id);
        evictActiveCache(policy.getLoanType());

        log.info("Activated policy: {} v{}", saved.getPolicyCode(), saved.getVersionNumber());
        return policyMapper.toResponse(saved);
    }

    @Override
    public PolicyResponse deactivate(String id, String modifiedBy) {
        log.info("Deactivating policy: {} by {}", id, modifiedBy);

        Policy policy = findPolicyById(id);
        policy.deactivate();
        policy.setModifiedBy(modifiedBy);

        Policy saved = policyRepository.save(policy);
        evictCache(id);
        evictActiveCache(policy.getLoanType());

        log.info("Deactivated policy: {} v{}", saved.getPolicyCode(), saved.getVersionNumber());
        return policyMapper.toResponse(saved);
    }

    @Override
    public void delete(String id) {
        log.info("Deleting policy: {}", id);

        Policy policy = findPolicyById(id);

        if (policy.getStatus() != PolicyStatus.DRAFT) {
            throw BusinessException.invalidOperation(
                    "Only DRAFT policies can be deleted. Current status: " + policy.getStatus());
        }

        policyRepository.delete(policy);
        evictCache(id);
        log.info("Deleted policy: {} (code: {})", id, policy.getPolicyCode());
    }

    // ==================== Query Operations ====================

    @Override
    public Page<PolicyResponse> listAll(Pageable pageable) {
        return policyRepository.findAll(pageable).map(policyMapper::toResponse);
    }

    @Override
    public Page<PolicyResponse> listByCategory(PolicyCategory category, Pageable pageable) {
        return policyRepository.findByCategory(category, pageable).map(policyMapper::toResponse);
    }

    @Override
    public Page<PolicyResponse> search(String searchText, Pageable pageable) {
        return policyRepository.searchByText(searchText, pageable).map(policyMapper::toResponse);
    }

    @Override
    public List<PolicyResponse> getActivePoliciesForLoanType(LoanType loanType) {
        // Try cache first
        String cacheKey = CACHE_ACTIVE_PREFIX + loanType.name();
        @SuppressWarnings("unchecked")
        List<PolicyResponse> cached = (List<PolicyResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for active policies: {}", loanType);
            return cached;
        }

        List<Policy> policies = policyRepository.findActivePoliciesForLoanType(loanType);
        List<PolicyResponse> responses = policyMapper.toResponseList(policies);

        // Cache the result
        try {
            redisTemplate.opsForValue().set(cacheKey, responses, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache active policies for {}: {}", loanType, e.getMessage());
        }

        return responses;
    }

    @Override
    public List<PolicyResponse> getActivePoliciesByCategoryAndLoanType(PolicyCategory category, LoanType loanType) {
        List<Policy> policies = policyRepository.findActivePoliciesByCategoryAndLoanType(category, loanType);
        return policyMapper.toResponseList(policies);
    }

    @Override
    public PolicyStatsResponse getStats() {
        long total = policyRepository.count();
        long active = countByStatus(PolicyStatus.ACTIVE);
        long draft = countByStatus(PolicyStatus.DRAFT);
        long inactive = countByStatus(PolicyStatus.INACTIVE);

        Map<String, Long> byCategory = new HashMap<>();
        for (PolicyCategory cat : PolicyCategory.values()) {
            long count = policyRepository.countByCategoryAndStatus(cat, PolicyStatus.ACTIVE);
            if (count > 0) {
                byCategory.put(cat.name(), count);
            }
        }

        return new PolicyStatsResponse(total, active, draft, inactive, byCategory);
    }

    // ==================== Private Helpers ====================

    private Policy findPolicyById(String id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + id));
    }

    private long countByStatus(PolicyStatus status) {
        return Arrays.stream(PolicyCategory.values())
                .mapToLong(cat -> policyRepository.countByCategoryAndStatus(cat, status))
                .sum();
    }

    private PolicyResponse getCachedResponse(String cacheKey) {
        try {
            return (PolicyResponse) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis cache read failed for {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void cacheResponse(String cacheKey, PolicyResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for {}: {}", cacheKey, e.getMessage());
        }
    }

    private void evictCache(String id) {
        try {
            redisTemplate.delete(CACHE_PREFIX + id);
        } catch (Exception e) {
            log.warn("Redis cache eviction failed for {}: {}", id, e.getMessage());
        }
    }

    private void evictActiveCache(LoanType loanType) {
        try {
            // Evict specific loan type cache and ALL cache
            redisTemplate.delete(CACHE_ACTIVE_PREFIX + loanType.name());
            redisTemplate.delete(CACHE_ACTIVE_PREFIX + "ALL");
        } catch (Exception e) {
            log.warn("Redis active cache eviction failed: {}", e.getMessage());
        }
    }
}
