package com.loanflow.loan.incomeverification.controller;

import com.loanflow.loan.incomeverification.dto.IncomeVerificationRequest;
import com.loanflow.loan.incomeverification.dto.IncomeVerificationResponse;
import com.loanflow.loan.incomeverification.service.IncomeVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for income verification.
 * Endpoints:
 * - POST /api/v1/income-verification/verify — trigger income verification
 * - GET /api/v1/income-verification/{pan} — get cached verification by PAN
 */
@RestController
@RequestMapping("/api/v1/income-verification")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
public class IncomeVerificationController {

    private final IncomeVerificationService incomeVerificationService;

    /**
     * Trigger income verification for an applicant.
     */
    @PostMapping("/verify")
    public ResponseEntity<IncomeVerificationResponse> verify(
            @RequestBody IncomeVerificationRequest request) {
        if (request.getPan() == null || request.getPan().isBlank()) {
            throw new IllegalArgumentException("PAN is required for income verification");
        }

        log.info("Income verification requested for PAN {}***",
                request.getPan().substring(0, Math.min(3, request.getPan().length())));

        IncomeVerificationResponse response = incomeVerificationService.verify(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get cached income verification result by PAN.
     */
    @GetMapping("/{pan}")
    public ResponseEntity<IncomeVerificationResponse> getCachedResult(@PathVariable String pan) {
        log.info("Fetching cached income verification for PAN {}***",
                pan.substring(0, Math.min(3, pan.length())));

        IncomeVerificationRequest request = IncomeVerificationRequest.builder()
                .pan(pan)
                .build();
        IncomeVerificationResponse response = incomeVerificationService.verify(request);
        return ResponseEntity.ok(response);
    }
}
