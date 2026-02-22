package com.loanflow.loan.service.impl;

import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.dto.CustomerLoanApplicationRequest;
import com.loanflow.loan.mapper.LoanApplicationMapper;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.LoanApplicationService;
import com.loanflow.loan.workflow.WorkflowService;
import com.loanflow.util.exception.BusinessException;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final LoanApplicationMapper mapper;
    private final WorkflowService workflowService;

    @Override
    public LoanApplicationResponse create(LoanApplicationRequest request) {
        log.info("Creating loan application for customer: {}", request.getCustomerId());

        LoanApplication application = mapper.toEntity(request);
        application.generateApplicationNumber();
        application.validate();

        LoanApplication saved = repository.save(application);
        log.info("Created loan application: {}", saved.getApplicationNumber());

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getByApplicationNumber(String applicationNumber) {
        return repository.findByApplicationNumber(applicationNumber)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LoanApplication", "applicationNumber", applicationNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getByCustomerId(UUID customerId, Pageable pageable) {
        return repository.findByCustomerId(customerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getByStatus(LoanStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getByAssignedOfficer(UUID officerId, Pageable pageable) {
        return repository.findByAssignedOfficer(officerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public LoanApplicationResponse update(UUID id, LoanApplicationRequest request) {
        LoanApplication application = findById(id);

        if (!application.isEditable()) {
            throw BusinessException.invalidOperation(
                    "Cannot update application in " + application.getStatus() + " status");
        }

        mapper.updateEntity(application, request);
        LoanApplication updated = repository.save(application);

        log.info("Updated loan application: {}", updated.getApplicationNumber());
        return mapper.toResponse(updated);
    }

    @Override
    public LoanApplicationResponse submit(UUID id) {
        LoanApplication application = findById(id);
        application.submit();

        LoanApplication saved = repository.save(application);
        log.info("Submitted loan application: {}", saved.getApplicationNumber());

        // Start Flowable workflow process
        String processInstanceId = workflowService.startProcess(saved.getId(), buildProcessVariables(saved));
        saved.setWorkflowInstanceId(processInstanceId);
        repository.save(saved);

        // TODO: Send notification to assigned officer

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse approve(UUID id, BigDecimal approvedAmount, BigDecimal interestRate) {
        LoanApplication application = findById(id);
        application.approve(approvedAmount, interestRate);

        LoanApplication saved = repository.save(application);
        log.info("Approved loan application: {} for amount: {}",
                saved.getApplicationNumber(), approvedAmount);

        // TODO: Send approval notification
        // TODO: Generate sanction letter

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse conditionallyApprove(UUID id, BigDecimal approvedAmount,
                                                         BigDecimal interestRate, String conditions) {
        LoanApplication application = findById(id);

        if (application.getStatus() != LoanStatus.UNDERWRITING) {
            throw BusinessException.invalidStatus(
                    application.getStatus().name(), LoanStatus.UNDERWRITING.name());
        }

        application.setApprovedAmount(approvedAmount);
        application.setInterestRate(interestRate);
        application.transitionTo(LoanStatus.CONDITIONALLY_APPROVED);
        // Store conditions in remarks or separate table

        LoanApplication saved = repository.save(application);
        log.info("Conditionally approved loan application: {}", saved.getApplicationNumber());

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse reject(UUID id, String reason) {
        LoanApplication application = findById(id);
        application.reject(reason);

        LoanApplication saved = repository.save(application);
        log.info("Rejected loan application: {} - Reason: {}",
                saved.getApplicationNumber(), reason);

        // TODO: Send rejection notification

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse returnForCorrection(UUID id, String reason) {
        LoanApplication application = findById(id);

        if (!application.getStatus().canTransitionTo(LoanStatus.RETURNED)) {
            throw BusinessException.invalidStatus(
                    application.getStatus().name(), LoanStatus.RETURNED.name());
        }

        application.transitionTo(LoanStatus.RETURNED);
        application.setRejectionReason(reason); // Using rejection reason field for return reason

        LoanApplication saved = repository.save(application);
        log.info("Returned loan application for correction: {}", saved.getApplicationNumber());

        // TODO: Send notification to customer

        return mapper.toResponse(saved);
    }

    @Override
    public void cancel(UUID id, String reason) {
        LoanApplication application = findById(id);

        // Cancel Flowable process if running
        if (application.getWorkflowInstanceId() != null) {
            try {
                workflowService.cancelProcess(application.getWorkflowInstanceId(), reason);
            } catch (Exception e) {
                log.warn("Failed to cancel workflow for application {}: {}",
                        application.getApplicationNumber(), e.getMessage());
            }
        }

        application.cancel(reason);
        repository.save(application);
        log.info("Cancelled loan application: {} - Reason: {}",
                application.getApplicationNumber(), reason);
    }

    @Override
    public LoanApplicationResponse assignOfficer(UUID id, UUID officerId) {
        LoanApplication application = findById(id);
        application.setAssignedOfficer(officerId);

        LoanApplication saved = repository.save(application);
        log.info("Assigned officer {} to application: {}",
                officerId, saved.getApplicationNumber());

        // TODO: Send notification to officer

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse transitionStatus(UUID id, LoanStatus newStatus) {
        LoanApplication application = findById(id);
        application.transitionTo(newStatus);

        LoanApplication saved = repository.save(application);
        log.info("Transitioned application {} to status: {}",
                saved.getApplicationNumber(), newStatus);

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse updateCibilScore(UUID id, Integer cibilScore, String riskCategory) {
        LoanApplication application = findById(id);
        application.setCibilScore(cibilScore);
        application.setRiskCategory(riskCategory);

        LoanApplication saved = repository.save(application);
        log.info("Updated CIBIL score for application {}: score={}, risk={}",
                saved.getApplicationNumber(), cibilScore, riskCategory);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> searchByApplicationNumber(String query, Pageable pageable) {
        log.debug("Searching loan applications with query: {}", query);
        return repository.searchByApplicationNumber(query, pageable)
                .map(mapper::toResponse);
    }

    // ==================== CUSTOMER PORTAL METHODS ====================
    // Issue: #26 [US-024] Customer Loan Application Form

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getByCustomerEmail(String email, Pageable pageable) {
        log.debug("Fetching loan applications for customer email: {}", email);
        return repository.findByCustomerEmail(email, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public LoanApplicationResponse createCustomerApplication(String customerEmail, CustomerLoanApplicationRequest request) {
        log.info("Customer {} applying for {} loan of amount {}",
                customerEmail, request.loanType(), request.requestedAmount());

        // Create loan application entity from customer request
        LoanApplication application = LoanApplication.builder()
                .customerId(UUID.randomUUID()) // Temporary - will be fetched from customer service
                .customerEmail(customerEmail)
                .loanType(request.loanType())
                .requestedAmount(request.requestedAmount())
                .tenureMonths(request.tenureMonths())
                .purpose(request.purpose())
                .status(LoanStatus.DRAFT)
                .build();

        application.generateApplicationNumber();
        application.validate();

        // Auto-submit customer applications (they don't need DRAFT state)
        application.submit();

        LoanApplication saved = repository.save(application);
        log.info("Customer {} submitted loan application: {}",
                customerEmail, saved.getApplicationNumber());

        // Start Flowable workflow process (pass PAN for credit bureau pull)
        String processInstanceId = workflowService.startProcess(
                saved.getId(), buildProcessVariables(saved, request.pan()));
        saved.setWorkflowInstanceId(processInstanceId);
        repository.save(saved);

        // TODO: Create/update customer record in customer-service
        // TODO: Send confirmation notification to customer

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse acceptOffer(UUID applicationId, String customerEmail) {
        LoanApplication application = findById(applicationId);

        // Verify ownership
        if (!customerEmail.equalsIgnoreCase(application.getCustomerEmail())) {
            throw BusinessException.accessDenied(
                    "You don't have permission to access this application");
        }

        // Verify application is in APPROVED status
        if (application.getStatus() != LoanStatus.APPROVED) {
            throw BusinessException.invalidOperation(
                    "Can only accept offer for approved applications. Current status: " + application.getStatus());
        }

        // Transition to disbursement pending
        application.transitionTo(LoanStatus.DISBURSEMENT_PENDING);

        LoanApplication saved = repository.save(application);
        log.info("Customer {} accepted loan offer for application: {}",
                customerEmail, saved.getApplicationNumber());

        // TODO: Trigger disbursement process
        // TODO: Generate loan agreement document
        // TODO: Send acceptance confirmation

        return mapper.toResponse(saved);
    }

    @Override
    public LoanApplicationResponse rejectOffer(UUID applicationId, String customerEmail, String reason) {
        LoanApplication application = findById(applicationId);

        // Verify ownership
        if (!customerEmail.equalsIgnoreCase(application.getCustomerEmail())) {
            throw BusinessException.accessDenied(
                    "You don't have permission to access this application");
        }

        // Verify application is in APPROVED status
        if (application.getStatus() != LoanStatus.APPROVED) {
            throw BusinessException.invalidOperation(
                    "Can only reject offer for approved applications. Current status: " + application.getStatus());
        }

        // Cancel the application with customer's rejection reason
        application.setRejectionReason("Customer rejected offer: " + reason);
        application.transitionTo(LoanStatus.CANCELLED);

        LoanApplication saved = repository.save(application);
        log.info("Customer {} rejected loan offer for application: {} - Reason: {}",
                customerEmail, saved.getApplicationNumber(), reason);

        // TODO: Send rejection confirmation

        return mapper.toResponse(saved);
    }

    // ==================== Private Methods ====================

    private LoanApplication findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", id));
    }

    private Map<String, Object> buildProcessVariables(LoanApplication application) {
        return buildProcessVariables(application, null);
    }

    private Map<String, Object> buildProcessVariables(LoanApplication application, String customerPan) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationNumber", application.getApplicationNumber());
        variables.put("loanType", application.getLoanType().name());
        variables.put("requestedAmount", application.getRequestedAmount().toString());
        variables.put("customerId", application.getCustomerId().toString());
        variables.put("customerEmail",
                application.getCustomerEmail() != null ? application.getCustomerEmail() : "");
        if (customerPan != null && !customerPan.isBlank()) {
            variables.put("customerPan", customerPan);
        }
        return variables;
    }
}
