package com.loanflow.loan.workflow.delegate;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Service task delegate: runs automatically after Document Verification is complete.
 * Checks customer KYC verification status via customer-service REST API.
 * This is a soft check — does NOT block the workflow even if KYC is not verified.
 * (US-029)
 */
@Component("kycCheckDelegate")
@RequiredArgsConstructor
@Slf4j
public class KycCheckDelegate implements JavaDelegate {

    private final LoanApplicationRepository repository;
    private final RestTemplate restTemplate;

    @Value("${loanflow.services.customer-url:http://localhost:8082}")
    private String customerServiceUrl;

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = (String) execution.getVariable("applicationId");
        log.info("Workflow [KycCheck]: Checking KYC status for application {}", applicationId);

        LoanApplication application = repository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        UUID customerId = application.getCustomerId();
        boolean kycVerified = false;
        String kycStatus = "UNKNOWN";

        try {
            String url = customerServiceUrl + "/api/v1/customers/" + customerId + "/ekyc/status";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                kycStatus = (String) data.getOrDefault("status", "UNKNOWN");
                kycVerified = "VERIFIED".equals(kycStatus);

                log.info("Workflow [KycCheck]: Customer {} KYC status = {}, verified = {}",
                        customerId, kycStatus, kycVerified);
            } else {
                log.warn("Workflow [KycCheck]: No data in KYC status response for customer {}", customerId);
            }
        } catch (Exception e) {
            log.warn("Workflow [KycCheck]: Failed to check KYC status for customer {} — proceeding anyway. Error: {}",
                    customerId, e.getMessage());
        }

        // Set process variables for downstream tasks
        execution.setVariable("kycStatus", kycStatus);
        execution.setVariable("kycVerified", kycVerified);

        log.info("Workflow [KycCheck]: Application {} — KYC status={}, verified={} (soft check, proceeding to credit check)",
                application.getApplicationNumber(), kycStatus, kycVerified);
    }
}
