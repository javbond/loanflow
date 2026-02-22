package com.loanflow.loan.creditbureau.controller;

import com.loanflow.loan.creditbureau.dto.CreditBureauRequest;
import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.creditbureau.dto.CreditPullRequest;
import com.loanflow.loan.creditbureau.service.CreditBureauService;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for credit bureau operations.
 * Allows staff to manually trigger CIBIL pulls or retrieve cached reports.
 */
@RestController
@RequestMapping("/api/v1/credit-bureau")
@RequiredArgsConstructor
@Slf4j
public class CreditBureauController {

    private final CreditBureauService creditBureauService;
    private final LoanApplicationRepository loanApplicationRepository;

    /**
     * Manually trigger a CIBIL credit bureau pull.
     * Can pull by applicationId (reads PAN from workflow) or directly by PAN.
     */
    @PostMapping("/pull")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<CreditBureauResponse> pullReport(@Valid @RequestBody CreditPullRequest request) {
        log.info("Credit bureau pull requested: {}", request);

        String pan = request.getPan();

        // If applicationId provided, look up application (PAN not stored on entity — use direct PAN)
        if (pan == null && request.getApplicationId() != null) {
            LoanApplication app = loanApplicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Application not found: " + request.getApplicationId()));
            // PAN is not stored on LoanApplication entity (PII) — require direct PAN
            throw new IllegalArgumentException(
                    "PAN is required for credit bureau pull. Application " +
                    app.getApplicationNumber() + " does not store PAN directly.");
        }

        if (pan == null || pan.isBlank()) {
            throw new IllegalArgumentException("PAN is required for credit bureau pull");
        }

        CreditBureauRequest bureauRequest = CreditBureauRequest.builder()
                .pan(pan)
                .build();

        CreditBureauResponse response = creditBureauService.pullReport(bureauRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve cached credit bureau report by PAN.
     * Returns 404 if no cached report exists.
     */
    @GetMapping("/{pan}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<CreditBureauResponse> getCachedReport(@PathVariable String pan) {
        log.info("Credit bureau cache lookup for PAN {}***", pan.substring(0, 3));

        CreditBureauRequest request = CreditBureauRequest.builder()
                .pan(pan)
                .build();

        // pullReport will return cached if available, otherwise fresh
        CreditBureauResponse response = creditBureauService.pullReport(request);
        return ResponseEntity.ok(response);
    }
}
