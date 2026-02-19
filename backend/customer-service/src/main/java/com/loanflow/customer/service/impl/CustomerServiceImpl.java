package com.loanflow.customer.service.impl;

import com.loanflow.customer.domain.entity.Customer;
import com.loanflow.customer.domain.enums.KycStatus;
import com.loanflow.customer.mapper.CustomerMapper;
import com.loanflow.customer.repository.CustomerRepository;
import com.loanflow.customer.service.CustomerService;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import com.loanflow.util.exception.DuplicateResourceException;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Override
    public CustomerResponse create(CustomerRequest request) {
        log.info("Creating customer: {} {}", request.getFirstName(), request.getLastName());

        // Check for duplicates
        validateUniqueness(request, null);

        // Map and validate
        Customer customer = mapper.toEntity(request);
        customer.generateCustomerNumber();
        customer.validate();

        Customer saved = repository.save(customer);
        log.info("Created customer: {}", saved.getCustomerNumber());

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getByCustomerNumber(String customerNumber) {
        return repository.findByCustomerNumber(customerNumber)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", "customerNumber", customerNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getByPan(String panNumber) {
        return repository.findByPanNumber(panNumber)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", "panNumber", panNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getByMobile(String mobileNumber) {
        return repository.findByMobileNumber(mobileNumber)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", "mobileNumber", mobileNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getByEmail(String email) {
        return repository.findByEmail(email)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = findById(id);

        // Check uniqueness excluding current customer
        validateUniqueness(request, id);

        mapper.updateEntity(customer, request);
        Customer updated = repository.save(customer);

        log.info("Updated customer: {}", updated.getCustomerNumber());
        return mapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchByName(String name, Pageable pageable) {
        return repository.searchByName(name, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchByQuery(String query, Pageable pageable) {
        log.debug("Searching customers by query: {}", query);
        return repository.searchByQuery(query, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getByKycStatus(KycStatus kycStatus, Pageable pageable) {
        return repository.findByKycStatus(kycStatus, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public CustomerResponse verifyAadhaar(UUID id) {
        Customer customer = findById(id);
        customer.verifyAadhaar();
        customer.updateKycStatus();

        Customer saved = repository.save(customer);
        log.info("Verified Aadhaar for customer: {}", saved.getCustomerNumber());

        return mapper.toResponse(saved);
    }

    @Override
    public CustomerResponse verifyPan(UUID id) {
        Customer customer = findById(id);
        customer.verifyPan();
        customer.updateKycStatus();

        Customer saved = repository.save(customer);
        log.info("Verified PAN for customer: {}", saved.getCustomerNumber());

        return mapper.toResponse(saved);
    }

    @Override
    public CustomerResponse updateKycStatus(UUID id) {
        Customer customer = findById(id);
        customer.updateKycStatus();

        Customer saved = repository.save(customer);
        log.info("Updated KYC status for customer: {} to {}",
                saved.getCustomerNumber(), saved.getKycStatus());

        return mapper.toResponse(saved);
    }

    @Override
    public CustomerResponse deactivate(UUID id, String reason) {
        Customer customer = findById(id);
        customer.deactivate(reason);

        Customer saved = repository.save(customer);
        log.info("Deactivated customer: {} - Reason: {}",
                saved.getCustomerNumber(), reason);

        return mapper.toResponse(saved);
    }

    @Override
    public CustomerResponse block(UUID id, String reason) {
        Customer customer = findById(id);
        customer.block(reason);

        Customer saved = repository.save(customer);
        log.warn("Blocked customer: {} - Reason: {}",
                saved.getCustomerNumber(), reason);

        return mapper.toResponse(saved);
    }

    @Override
    public CustomerResponse reactivate(UUID id) {
        Customer customer = findById(id);
        customer.reactivate();

        Customer saved = repository.save(customer);
        log.info("Reactivated customer: {}", saved.getCustomerNumber());

        return mapper.toResponse(saved);
    }

    // ==================== Private Methods ====================

    private Customer findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    private void validateUniqueness(CustomerRequest request, UUID excludeId) {
        // Check PAN uniqueness
        if (request.getPanNumber() != null) {
            repository.findByPanNumber(request.getPanNumber())
                    .filter(c -> !c.getId().equals(excludeId))
                    .ifPresent(c -> {
                        throw new DuplicateResourceException("Customer", "PAN number", request.getPanNumber());
                    });
        }

        // Check Aadhaar uniqueness
        if (request.getAadhaarNumber() != null) {
            repository.findByAadhaarNumber(request.getAadhaarNumber())
                    .filter(c -> !c.getId().equals(excludeId))
                    .ifPresent(c -> {
                        throw new DuplicateResourceException("Customer", "Aadhaar number", request.getAadhaarNumber());
                    });
        }

        // Check email uniqueness
        if (request.getEmail() != null) {
            repository.findByEmail(request.getEmail())
                    .filter(c -> !c.getId().equals(excludeId))
                    .ifPresent(c -> {
                        throw new DuplicateResourceException("Customer", "email", request.getEmail());
                    });
        }

        // Check mobile uniqueness
        if (request.getMobileNumber() != null) {
            repository.findByMobileNumber(request.getMobileNumber())
                    .filter(c -> !c.getId().equals(excludeId))
                    .ifPresent(c -> {
                        throw new DuplicateResourceException("Customer", "mobile number", request.getMobileNumber());
                    });
        }
    }
}
