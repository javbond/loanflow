package com.loanflow.loan.service.impl;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.DocumentGenerationService;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.xhtmlrenderer.pdf.ITextRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * Thymeleaf + Flying Saucer based PDF generation service (US-023).
 * Renders sanction letter HTML template and converts to PDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThymeleafDocumentGenerationService implements DocumentGenerationService {

    private static final Set<LoanStatus> ALLOWED_STATUSES = Set.of(
            LoanStatus.APPROVED,
            LoanStatus.DISBURSED
    );

    private final LoanApplicationRepository repository;
    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateSanctionLetter(UUID applicationId) {
        log.info("Generating sanction letter for application: {}", applicationId);

        LoanApplication loan = repository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", applicationId.toString()));

        // Validate loan status
        if (!ALLOWED_STATUSES.contains(loan.getStatus())) {
            throw new IllegalStateException(
                    "Cannot generate sanction letter for loan in status: " + loan.getStatus() +
                    ". Loan must be APPROVED or DISBURSED.");
        }

        // Build Thymeleaf context
        Context context = new Context();
        context.setVariable("applicationNumber", loan.getApplicationNumber());
        context.setVariable("customerName", loan.getCustomerEmail()); // Use email as fallback
        context.setVariable("loanType", formatLoanType(loan.getLoanType().name()));
        context.setVariable("approvedAmount", loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getRequestedAmount());
        context.setVariable("interestRate", loan.getInterestRate() != null ? loan.getInterestRate() : BigDecimal.ZERO);
        context.setVariable("tenureMonths", loan.getTenureMonths());
        context.setVariable("emiAmount", loan.getEmiAmount() != null ? loan.getEmiAmount() : BigDecimal.ZERO);
        context.setVariable("processingFee", loan.getProcessingFee());
        context.setVariable("purpose", loan.getPurpose());
        context.setVariable("cibilScore", loan.getCibilScore());
        context.setVariable("branchCode", loan.getBranchCode());
        context.setVariable("sanctionDate", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        // Render HTML
        String html = templateEngine.process("sanction-letter", context);

        // Convert HTML to PDF
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);

            byte[] pdf = out.toByteArray();
            log.info("Sanction letter generated: {} bytes for application {}", pdf.length, applicationId);
            return pdf;
        } catch (Exception e) {
            log.error("Failed to generate sanction letter PDF for {}: {}", applicationId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate sanction letter PDF", e);
        }
    }

    private String formatLoanType(String loanType) {
        return switch (loanType) {
            case "HOME_LOAN" -> "Home Loan";
            case "PERSONAL_LOAN" -> "Personal Loan";
            case "VEHICLE_LOAN" -> "Vehicle Loan";
            case "BUSINESS_LOAN" -> "Business Loan";
            case "GOLD_LOAN" -> "Gold Loan";
            case "EDUCATION_LOAN" -> "Education Loan";
            default -> loanType.replace("_", " ");
        };
    }
}
