package com.loanflow.loan.controller;

import com.loanflow.loan.service.DocumentGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for document generation (US-023).
 * Generates PDF documents such as sanction letters for approved loans.
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Generation", description = "APIs for generating loan-related PDF documents")
public class DocumentGenerationController {

    private final DocumentGenerationService documentGenerationService;

    @GetMapping("/{id}/generate/sanction-letter")
    @Operation(summary = "Generate sanction letter PDF for an approved loan")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'ADMIN')")
    public ResponseEntity<byte[]> generateSanctionLetter(@PathVariable UUID id) {
        log.info("Generating sanction letter for loan: {}", id);

        byte[] pdfBytes = documentGenerationService.generateSanctionLetter(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "sanction-letter-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
